#!/usr/bin/env bash
# US-008 (SCRUM-60), open item OI-03: checks that each AWS service of the stack (03.04.01, 03.07.01) can be used
# in an AWS Academy Learner Lab account with LabRole. For every service it creates the smallest resource it can,
# uses it once and deletes it, then writes one line to a CSV: WORKS, LIMITED (works with a limit noted in the
# detail), BLOCKED (access denied) or FAILED (another error, read the detail).
#
# Run it in the terminal of the Learner Lab page, which already has the AWS CLI and the session credentials:
#   bash check-services.sh            quick checks, about 10 minutes, a few cents
#   ONLY="bedrock s3" bash check-services.sh   only the named checks
#   AURORA_CLASSES="db.r6g.large" or BEDROCK_REGION=us-west-2   other instance classes or Bedrock region
#   bash check-services.sh --full     adds VPC Link, ECS Fargate, RDS, Aurora Global, EKS and Elastic Beanstalk;
#                                     about 90 minutes, around USD 1 to 2 of the lab budget
# Every resource is named vigia-check-<suffix> and is deleted at the end of its check, also when the script is
# stopped with Ctrl+C. Nothing is left running. Not covered here: Transit Gateway (checked on 2026-11-20),
# CloudFront and ACM (need the domain, OI-02), cross-account delivery PRD-1 to PRD-2 (needs two accounts) and
# the Keycloak checks K1 to K5 (ADR-050, with US-219).
set -uo pipefail

REGION="us-east-1"
STANDBY="us-west-2"
FULL=false
[[ "${1:-}" == "--full" ]] && FULL=true
export AWS_DEFAULT_REGION="$REGION" AWS_PAGER=""

SUFFIX="$(date +%m%d%H%M)"
NAME="vigia-check-${SUFFIX}"
RESULTS="results-${SUFFIX}.csv"
ERRF="$(mktemp)"
WORK="$(mktemp -d)"
CLEANUP=()

echo "service,result,detail" > "$RESULTS"

# --- helpers -----------------------------------------------------------------------------------------------------

record() {
  local detail="${3//\"/\'}"
  printf '%s,%s,"%s"\n' "$1" "$2" "$detail" >> "$RESULTS"
  printf '%-30s %-8s %s\n' "$1" "$2" "$3"
}

# Runs an AWS CLI command; its error text is kept in $ERRF for fail().
run() { "$@" > /dev/null 2>"$ERRF"; }

fail() {
  local result="FAILED"
  grep -qiE 'AccessDenied|not authorized|UnauthorizedOperation|explicit deny|is not authorized|missing the following permissions' "$ERRF" && result="BLOCKED"
  record "$1" "$result" "$(tr '\n' ' ' < "$ERRF" | cut -c1-600)"
}

# Registers a delete command; undo runs the pending ones in reverse order.
defer() { CLEANUP+=("$1"); }
undo() {
  local i
  for (( i=${#CLEANUP[@]}-1; i>=0; i-- )); do
    eval "${CLEANUP[$i]}" > /dev/null 2>&1 || echo "  cleanup needs a second look: ${CLEANUP[$i]}"
  done
  CLEANUP=()
}
trap 'echo; undo; rm -rf "$WORK" "$ERRF"' EXIT

wait_for() { # wait_for <seconds> <description> <command that prints the status> <expected status>
  local limit=$1 what=$2 cmd=$3 want=$4 waited=0 status
  while (( waited < limit )); do
    status="$(eval "$cmd" 2>/dev/null)"
    [[ "$status" == "$want" ]] && return 0
    sleep 15; waited=$(( waited + 15 ))
  done
  echo "  timed out waiting for $what (last status: ${status:-none})" > "$ERRF"
  return 1
}

# --- context -----------------------------------------------------------------------------------------------------

ACCOUNT="$(aws sts get-caller-identity --query Account --output text)" || { echo "No AWS session: start the lab first."; exit 1; }
CALLER="$(aws sts get-caller-identity --query Arn --output text)"
# The Learner Lab account is used through the role voclabs. Any other role (for example the terminal host role
# "runserver") is not the lab account, and every check would report BLOCKED for the wrong reason.
if [[ "$CALLER" != *"/voclabs/"* && "$CALLER" != *"/LabRole/"* && "${ALLOW_ANY_ROLE:-}" != "yes" ]]; then
  echo "The AWS CLI is using ${CALLER}, not the Learner Lab role voclabs."
  echo "Copy the AWS CLI credentials from 'AWS Details' into ~/.aws/credentials, then run again."
  exit 1
fi
# The checks need a recent AWS CLI v2 (bedrock, rekognition create-face-liveness-session, ECR pull-through cache).
if ! aws bedrock help > /dev/null 2>&1 || ! aws ecr create-pull-through-cache-rule help > /dev/null 2>&1; then
  echo "This AWS CLI ($(aws --version 2>&1)) is too old for these checks. Install the current AWS CLI v2:"
  echo "  curl -sSLo awscliv2.zip https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip && unzip -q awscliv2.zip"
  echo "  ./aws/install -i ~/aws-cli -b ~/bin && export PATH=~/bin:\$PATH && aws --version"
  exit 1
fi
LAB_ROLE="arn:aws:iam::${ACCOUNT}:role/LabRole"
VPC="$(aws ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text)"
read -r SUBNET_A SUBNET_B <<< "$(aws ec2 describe-subnets \
  --filters Name=vpc-id,Values="$VPC" Name=availability-zone,Values=${REGION}a,${REGION}b \
  --query 'Subnets[].SubnetId' --output text)"
echo "Account ${ACCOUNT}, region ${REGION}, default VPC ${VPC}, subnets ${SUBNET_A} ${SUBNET_B}, mode $([[ $FULL == true ]] && echo full || echo quick)"
echo "Results: ${RESULTS}"
echo

# --- quick checks --------------------------------------------------------------------------------------------------

check_quotas() {
  local vcpu
  vcpu="$(aws service-quotas get-service-quota --service-code ec2 --quota-code L-1216C47A \
    --query 'Quota.Value' --output text 2>"$ERRF")" && record "EC2 vCPU quota" LIMITED "On-Demand standard vCPU quota ${vcpu} (constraint: 9 instances, 32 vCPU)" \
    || record "EC2 vCPU quota" LIMITED "quota not readable; Learner Lab documents 9 instances and 32 vCPU per region"
}

check_s3() {
  local b="${NAME}-objlock"
  if run aws s3api create-bucket --bucket "$b" --object-lock-enabled-for-bucket; then
    defer "aws s3api delete-bucket --bucket $b"
    if run aws s3api put-object-lock-configuration --bucket "$b" \
      --object-lock-configuration '{"ObjectLockEnabled":"Enabled","Rule":{"DefaultRetention":{"Mode":"GOVERNANCE","Days":1}}}'; then
      record "S3 Object Lock" WORKS "bucket with Object Lock and default retention created and deleted"
    else fail "S3 Object Lock"; fi
  else fail "S3 Object Lock"; fi
  undo
  # Fallback for the audit chain (ADR-020) if Object Lock is blocked: versioning plus a policy that denies
  # deleting object versions.
  b="${NAME}-versioned"
  if run aws s3api create-bucket --bucket "$b"; then
    defer "aws s3api delete-bucket --bucket $b"
    if run aws s3api put-bucket-versioning --bucket "$b" --versioning-configuration Status=Enabled \
      && run aws s3api put-bucket-policy --bucket "$b" --policy \
        "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Deny\",\"Principal\":\"*\",\"Action\":[\"s3:DeleteObjectVersion\"],\"Resource\":\"arn:aws:s3:::$b/*\"}]}"; then
      defer "aws s3api delete-bucket-policy --bucket $b"
      record "S3 versioning and deny policy" WORKS "versioned bucket that denies deleting object versions (fallback for Object Lock)"
    else fail "S3 versioning and deny policy"; fi
  else fail "S3 versioning and deny policy"; fi
  undo
}

check_dynamodb() {
  local t="${NAME}-table"
  if run aws dynamodb create-table --table-name "$t" --billing-mode PAY_PER_REQUEST \
    --attribute-definitions AttributeName=pk,AttributeType=S --key-schema AttributeName=pk,KeyType=HASH \
    --stream-specification StreamEnabled=true,StreamViewType=NEW_AND_OLD_IMAGES; then
    defer "aws dynamodb delete-table --table-name $t"
    aws dynamodb wait table-exists --table-name "$t"
    if run aws dynamodb put-item --table-name "$t" --item '{"pk":{"S":"a"}}' --condition-expression 'attribute_not_exists(pk)'; then
      record "DynamoDB" WORKS "on-demand table, stream and conditional write"
    else fail "DynamoDB"; fi
    if run aws dynamodb update-continuous-backups --table-name "$t" \
      --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true; then
      record "DynamoDB PITR" WORKS "point-in-time recovery enabled"
    else fail "DynamoDB PITR"; fi
    if run aws dynamodb update-table --table-name "$t" --replica-updates "[{\"Create\":{\"RegionName\":\"${STANDBY}\"}}]"; then
      record "DynamoDB Global Tables" WORKS "replica in ${STANDBY} requested (ADR-049)"
      wait_for 600 "replica" "aws dynamodb describe-table --table-name $t --query 'Table.Replicas[0].ReplicaStatus' --output text" ACTIVE
      aws dynamodb update-table --table-name "$t" --replica-updates "[{\"Delete\":{\"RegionName\":\"${STANDBY}\"}}]" > /dev/null 2>&1
      wait_for 600 "replica removal" "aws dynamodb describe-table --table-name $t --query 'length(Table.Replicas || \`[]\`)' --output text" 0
    else fail "DynamoDB Global Tables"; fi
  else fail "DynamoDB"; fi
  undo
}

check_secrets_kms() {
  local s="${NAME}-secret" key
  if run aws secretsmanager create-secret --name "$s" --secret-string 'check'; then
    defer "aws secretsmanager delete-secret --secret-id $s --force-delete-without-recovery"
    record "Secrets Manager" WORKS "secret created and deleted"
  else fail "Secrets Manager"; fi
  if key="$(aws kms create-key --description "$NAME" --query KeyMetadata.KeyId --output text 2>"$ERRF")"; then
    defer "aws kms schedule-key-deletion --key-id $key --pending-window-in-days 7"
    record "KMS" WORKS "customer managed key created; deletion scheduled (7 days, no charge while pending)"
  else fail "KMS"; fi
  undo
}

check_lambda_scheduler() {
  local f="${NAME}-fn" arn
  printf 'def handler(event, context):\n    return {"ok": True}\n' > "$WORK/index.py"
  (cd "$WORK" && python3 -m zipfile -c fn.zip index.py)
  if arn="$(aws lambda create-function --function-name "$f" --runtime python3.12 --handler index.handler \
    --role "$LAB_ROLE" --zip-file "fileb://$WORK/fn.zip" --query FunctionArn --output text 2>"$ERRF")"; then
    defer "aws lambda delete-function --function-name $f"
    aws lambda wait function-active-v2 --function-name "$f"
    if run aws lambda invoke --function-name "$f" "$WORK/out.json"; then
      record "Lambda" WORKS "function with LabRole created and invoked"
    else fail "Lambda"; fi
    if run aws scheduler create-schedule --name "$NAME" --schedule-expression 'rate(1 day)' \
      --flexible-time-window Mode=OFF --target "Arn=${arn},RoleArn=${LAB_ROLE}"; then
      defer "aws scheduler delete-schedule --name $NAME"
      record "EventBridge Scheduler" WORKS "schedule targeting Lambda with LabRole"
    else fail "EventBridge Scheduler"; fi
  else fail "Lambda"; fi
  undo
}

check_api_gateway() {
  local api
  if api="$(aws apigatewayv2 create-api --name "$NAME" --protocol-type HTTP --query ApiId --output text 2>"$ERRF")"; then
    defer "aws apigatewayv2 delete-api --api-id $api"
    if run aws apigatewayv2 create-authorizer --api-id "$api" --name keycloak --authorizer-type JWT \
      --identity-source '$request.header.Authorization' \
      --jwt-configuration Audience=vigia-api,Issuer=https://accounts.google.com; then
      record "API Gateway HTTP API" WORKS "HTTP API with JWT authorizer (public test issuer; the Keycloak issuer is K2, with US-219)"
    else fail "API Gateway HTTP API"; fi
  else fail "API Gateway HTTP API"; fi
  undo
}

check_iot() {
  local cert_id endpoint
  if endpoint="$(aws iot describe-endpoint --endpoint-type iot:Data-ATS --query endpointAddress --output text 2>"$ERRF")" \
    && run aws iot create-thing --thing-name "$NAME"; then
    defer "aws iot delete-thing --thing-name $NAME"
    if cert_id="$(aws iot create-keys-and-certificate --set-as-active \
      --query certificateId --output text 2>"$ERRF")" && [[ -n "$cert_id" ]]; then
      defer "aws iot delete-certificate --certificate-id $cert_id"
      defer "aws iot update-certificate --certificate-id $cert_id --new-status INACTIVE"
      record "IoT Core" WORKS "endpoint ${endpoint}; thing and X.509 certificate"
    else fail "IoT Core"; fi
    if run aws iot create-topic-rule --rule-name "vigia_check_${SUFFIX}" --topic-rule-payload \
      '{"sql":"SELECT * FROM '"'"'vigia/check'"'"'","ruleDisabled":true,"actions":[{"http":{"url":"https://example.org/ingest"}}]}'; then
      defer "aws iot delete-topic-rule --rule-name vigia_check_${SUFFIX}"
      record "IoT rule HTTPS action" WORKS "rule with an HTTPS action created (destination must be confirmed by the bridge, ADR-039)"
    else fail "IoT rule HTTPS action"; fi
  else fail "IoT Core"; fi
  undo
  local dest
  dest="$(aws iot list-topic-rule-destinations --query "destinationSummaries[?httpUrlSummary.confirmationUrl=='https://example.org/ingest'].arn" --output text 2>/dev/null)"
  [[ -n "$dest" && "$dest" != "None" ]] && aws iot delete-topic-rule-destination --arn "$dest" > /dev/null 2>&1
}

check_rekognition() {
  local c="${NAME}-faces"
  if run aws rekognition create-collection --collection-id "$c"; then
    defer "aws rekognition delete-collection --collection-id $c"
    record "Rekognition collection" WORKS "face collection created and deleted (ADR-016)"
  else fail "Rekognition collection"; fi
  if run aws rekognition create-face-liveness-session; then
    record "Rekognition Face Liveness" WORKS "liveness session created"
  else fail "Rekognition Face Liveness"; fi
  undo
}

check_bedrock() {
  # Listing models and invoking them are separate permissions; S-13 needs only the Converse call (ADR-022).
  local model tried=""
  for model in amazon.nova-micro-v1:0 amazon.nova-lite-v1:0 us.amazon.nova-micro-v1:0 us.amazon.nova-lite-v1:0 \
    us.anthropic.claude-3-5-haiku-20241022-v1:0 meta.llama3-8b-instruct-v1:0; do
    if run aws bedrock-runtime converse --region "${BEDROCK_REGION:-$REGION}" --model-id "$model" \
      --messages '[{"role":"user","content":[{"text":"Reply with OK"}]}]'; then
      record "Bedrock Converse" WORKS "Converse API answered with ${model}"
      return
    fi
    tried+="${model}: $(grep -oE '\(([A-Za-z]+Exception|AccessDenied)\)[^.]*' "$ERRF" | head -1 | cut -c1-90); "
  done
  echo "$tried" > "$ERRF"
  fail "Bedrock Converse"
}

check_data_lake() {
  local b="${NAME}-lake" q
  if ! run aws s3api create-bucket --bucket "$b"; then fail "Athena and Glue"; return; fi
  defer "aws s3api delete-bucket --bucket $b"
  defer "aws s3 rm s3://$b --recursive"
  if run aws glue create-database --database-input "Name=vigia_check_${SUFFIX}"; then
    defer "aws glue delete-database --name vigia_check_${SUFFIX}"
    record "Glue" WORKS "Data Catalog database"
  else fail "Glue"; fi
  if q="$(aws athena start-query-execution --query-string 'SELECT 1' \
    --result-configuration "OutputLocation=s3://$b/athena/" --query QueryExecutionId --output text 2>"$ERRF")"; then
    wait_for 120 "Athena query" "aws athena get-query-execution --query-execution-id $q --query QueryExecution.Status.State --output text" SUCCEEDED \
      && record "Athena" WORKS "SELECT 1 succeeded" || fail "Athena"
  else fail "Athena"; fi
  if run aws firehose create-delivery-stream --delivery-stream-name "$NAME" \
    --s3-destination-configuration "RoleARN=${LAB_ROLE},BucketARN=arn:aws:s3:::$b"; then
    defer "aws firehose delete-delivery-stream --delivery-stream-name $NAME"
    wait_for 300 "Firehose" "aws firehose describe-delivery-stream --delivery-stream-name $NAME --query DeliveryStreamDescription.DeliveryStreamStatus --output text" ACTIVE \
      && record "Data Firehose" WORKS "delivery stream to S3 with LabRole" || fail "Data Firehose"
  else fail "Data Firehose"; fi
  undo
}

check_ecr() {
  local p="vigia-check-${SUFFIX}"
  if run aws ecr create-repository --repository-name "$p/app"; then
    defer "aws ecr delete-repository --repository-name $p/app --force"
    record "ECR" WORKS "private repository"
  else fail "ECR"; fi
  if run aws ecr create-pull-through-cache-rule --ecr-repository-prefix "$p-cache" --upstream-registry-url public.ecr.aws; then
    defer "aws ecr delete-pull-through-cache-rule --ecr-repository-prefix $p-cache"
    record "ECR pull-through cache" LIMITED "rule works with public.ecr.aws; the GHCR rule needs a GitHub token in Secrets Manager (US-208)"
  else fail "ECR pull-through cache"; fi
  undo
}

check_observability() {
  local g="/vigia/check/${SUFFIX}"
  if run aws logs create-log-group --log-group-name "$g"; then
    defer "aws logs delete-log-group --log-group-name $g"
    record "CloudWatch Logs" WORKS "log group"
  else fail "CloudWatch Logs"; fi
  local now trace
  now="$(date +%s)"
  trace="1-$(printf '%x' "$now")-$(openssl rand -hex 12)"
  run aws xray put-trace-segments --trace-segment-documents \
    "{\"id\":\"$(openssl rand -hex 8)\",\"trace_id\":\"$trace\",\"name\":\"vigia-check\",\"start_time\":$now,\"end_time\":$((now + 1))}" \
    && record "X-Ray" WORKS "trace segment accepted (what the OpenTelemetry exporter sends)" || fail "X-Ray"
  run aws cloudtrail describe-trails && record "CloudTrail" WORKS "trails readable" || fail "CloudTrail"
  undo
}

# --- full checks (slow, billed by the hour while they run) ----------------------------------------------------------

check_vpc_link() {
  local id
  if id="$(aws apigatewayv2 create-vpc-link --name "$NAME" --subnet-ids "$SUBNET_A" "$SUBNET_B" --query VpcLinkId --output text 2>"$ERRF")"; then
    defer "aws apigatewayv2 delete-vpc-link --vpc-link-id $id"
    wait_for 600 "VPC Link" "aws apigatewayv2 get-vpc-link --vpc-link-id $id --query VpcLinkStatus --output text" AVAILABLE \
      && record "API Gateway VPC Link" WORKS "VPC Link available in two zones" || fail "API Gateway VPC Link"
  else fail "API Gateway VPC Link"; fi
  undo
}

check_fargate() {
  local task="" code
  if ! run aws ecs create-cluster --cluster-name "$NAME"; then fail "ECS Fargate"; return; fi
  defer "aws ecs delete-cluster --cluster $NAME"
  if run aws ecs register-task-definition --family "$NAME" --requires-compatibilities FARGATE --network-mode awsvpc \
    --cpu 256 --memory 512 --execution-role-arn "$LAB_ROLE" \
    --container-definitions '[{"name":"check","image":"public.ecr.aws/docker/library/busybox:stable","command":["echo","ok"],"essential":true}]'; then
    defer "aws ecs deregister-task-definition --task-definition $NAME:1"
    # The first run in a new account can fail while AWS creates the ECS service-linked role; retry a few times.
    local attempt
    for attempt in 1 2 3 4; do
      task="$(aws ecs run-task --cluster "$NAME" --launch-type FARGATE --task-definition "$NAME" \
        --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_A],assignPublicIp=ENABLED}" \
        --query 'tasks[0].taskArn' --output text 2>"$ERRF")" && break
      grep -q "service linked role" "$ERRF" || break
      echo "  ECS service-linked role not ready (attempt ${attempt}); retrying in 30 s"
      sleep 30
    done
    if [[ -n "$task" && "$task" != "None" ]]; then
      aws ecs wait tasks-stopped --cluster "$NAME" --tasks "$task"
      code="$(aws ecs describe-tasks --cluster "$NAME" --tasks "$task" --query 'tasks[0].containers[0].exitCode' --output text)"
      [[ "$code" == "0" ]] && record "ECS Fargate" WORKS "task with LabRole ran and exited 0 (n8n, ADR-048)" \
        || record "ECS Fargate" FAILED "task exit code ${code}"
    else fail "ECS Fargate"; fi
  else fail "ECS Fargate"; fi
  undo
}

check_rds() {
  local db="${NAME}-pg"
  if run aws rds create-db-instance --db-instance-identifier "$db" --engine postgres --db-instance-class db.t3.micro \
    --allocated-storage 20 --master-username vigia --manage-master-user-password --no-publicly-accessible \
    --backup-retention-period 0; then
    defer "aws rds delete-db-instance --db-instance-identifier $db --skip-final-snapshot --delete-automated-backups"
    aws rds wait db-instance-available --db-instance-identifier "$db" \
      && record "RDS PostgreSQL" WORKS "db.t3.micro instance available (S-04, n8n)" || fail "RDS PostgreSQL"
  else fail "RDS PostgreSQL"; fi
  undo
  aws rds wait db-instance-deleted --db-instance-identifier "$db" 2>/dev/null
}

check_aurora() {
  # Aurora PostgreSQL for the general and commercial clusters (ADR-003): Serverless v2 first, then the small
  # provisioned classes if Serverless is not allowed. Then the same cluster becomes a Global Database with a
  # secondary in the standby region (ADR-049, A-06).
  local g="${NAME}-global" p="${NAME}-primary" s="${NAME}-standby" pw class="" tried="" c
  pw="$(openssl rand -hex 16)"
  if ! run aws rds create-db-cluster --db-cluster-identifier "$p" --engine aurora-postgresql \
    --master-username vigia --master-user-password "$pw" --serverless-v2-scaling-configuration MinCapacity=0.5,MaxCapacity=1; then
    fail "Aurora PostgreSQL"
    return
  fi
  defer "aws rds wait db-cluster-deleted --db-cluster-identifier $p"
  defer "aws rds delete-db-cluster --db-cluster-identifier $p --skip-final-snapshot"
  for c in ${AURORA_CLASSES:-db.serverless db.t4g.medium db.t3.medium}; do
    if run aws rds create-db-instance --db-instance-identifier "$p-1" --db-cluster-identifier "$p" \
      --engine aurora-postgresql --db-instance-class "$c"; then
      class="$c"
      break
    fi
    tried+="${c}: $(grep -oE '\(([A-Za-z]+Exception|AccessDenied)\)[^.]*' "$ERRF" | head -1 | cut -c1-120); "
  done
  if [[ -z "$class" ]]; then
    echo "$tried" > "$ERRF"
    fail "Aurora PostgreSQL"
    undo
    return
  fi
  defer "aws rds wait db-instance-deleted --db-instance-identifier $p-1"
  defer "aws rds delete-db-instance --db-instance-identifier $p-1"
  if ! aws rds wait db-instance-available --db-instance-identifier "$p-1"; then
    fail "Aurora PostgreSQL"
    undo
    return
  fi
  if [[ "$class" == "db.serverless" ]]; then
    record "Aurora PostgreSQL" WORKS "Serverless v2 cluster, 0.5 to 1 ACU"
  else
    record "Aurora PostgreSQL" LIMITED "Serverless v2 not allowed (${tried}); works with ${class}"
  fi
  if run aws rds create-global-cluster --global-cluster-identifier "$g" \
    --source-db-cluster-identifier "arn:aws:rds:${REGION}:${ACCOUNT}:cluster:$p"; then
    defer "aws rds delete-global-cluster --global-cluster-identifier $g"
    defer "aws rds remove-from-global-cluster --global-cluster-identifier $g --db-cluster-identifier arn:aws:rds:${REGION}:${ACCOUNT}:cluster:$p"
    wait_for 900 "global cluster" "aws rds describe-global-clusters --global-cluster-identifier $g --query 'GlobalClusters[0].Status' --output text" available
    if run aws rds create-db-cluster --region "$STANDBY" --db-cluster-identifier "$s" --engine aurora-postgresql \
      --global-cluster-identifier "$g" --serverless-v2-scaling-configuration MinCapacity=0.5,MaxCapacity=1; then
      defer "aws rds wait db-cluster-deleted --region $STANDBY --db-cluster-identifier $s"
      defer "aws rds delete-db-cluster --region $STANDBY --db-cluster-identifier $s --skip-final-snapshot"
      defer "aws rds remove-from-global-cluster --region $STANDBY --global-cluster-identifier $g --db-cluster-identifier arn:aws:rds:${STANDBY}:${ACCOUNT}:cluster:$s"
      if run aws rds create-db-instance --region "$STANDBY" --db-instance-identifier "$s-1" --db-cluster-identifier "$s" \
        --engine aurora-postgresql --db-instance-class "$class"; then
        defer "aws rds wait db-instance-deleted --region $STANDBY --db-instance-identifier $s-1"
        defer "aws rds delete-db-instance --region $STANDBY --db-instance-identifier $s-1"
        aws rds wait db-instance-available --region "$STANDBY" --db-instance-identifier "$s-1" \
          && record "Aurora Global Database" WORKS "secondary cluster in ${STANDBY} with ${class} (A-06, ADR-049)" \
          || fail "Aurora Global Database"
      else fail "Aurora Global Database"; fi
    else fail "Aurora Global Database"; fi
  else fail "Aurora Global Database"; fi
  undo
}

check_eks() {
  if ! run aws eks create-cluster --name "$NAME" --role-arn "$LAB_ROLE" \
    --resources-vpc-config "subnetIds=${SUBNET_A},${SUBNET_B}"; then fail "EKS"; return; fi
  defer "aws eks wait cluster-deleted --name $NAME"
  defer "aws eks delete-cluster --name $NAME"
  if aws eks wait cluster-active --name "$NAME"; then
    if run aws eks create-nodegroup --cluster-name "$NAME" --nodegroup-name general --node-role "$LAB_ROLE" \
      --subnets "$SUBNET_A" "$SUBNET_B" --instance-types t3.large --scaling-config minSize=1,maxSize=1,desiredSize=1; then
      defer "aws eks wait nodegroup-deleted --cluster-name $NAME --nodegroup-name general"
      defer "aws eks delete-nodegroup --cluster-name $NAME --nodegroup-name general"
      aws eks wait nodegroup-active --cluster-name "$NAME" --nodegroup-name general \
        && record "EKS" WORKS "cluster and t3.large managed node group with LabRole" || fail "EKS"
    else fail "EKS"; fi
  else fail "EKS"; fi
  undo
}

check_beanstalk() {
  local stack status
  stack="$(aws elasticbeanstalk list-available-solution-stacks \
    --query "SolutionStacks[?contains(@, 'Corretto 21')] | [0]" --output text 2>"$ERRF")"
  if [[ -z "$stack" || "$stack" == "None" ]]; then fail "Elastic Beanstalk"; return; fi
  if ! run aws elasticbeanstalk create-application --application-name "$NAME"; then fail "Elastic Beanstalk"; return; fi
  defer "aws elasticbeanstalk delete-application --application-name $NAME --terminate-env-by-force"
  if run aws elasticbeanstalk create-environment --application-name "$NAME" --environment-name "$NAME" \
    --solution-stack-name "$stack" --option-settings \
    Namespace=aws:autoscaling:launchconfiguration,OptionName=IamInstanceProfile,Value=LabInstanceProfile \
    Namespace=aws:elasticbeanstalk:environment,OptionName=EnvironmentType,Value=SingleInstance \
    Namespace=aws:elasticbeanstalk:environment,OptionName=ServiceRole,Value=LabRole \
    Namespace=aws:ec2:instances,OptionName=InstanceTypes,Value=t3.micro; then
    defer "aws elasticbeanstalk wait environment-terminated --environment-names $NAME"
    defer "aws elasticbeanstalk terminate-environment --environment-name $NAME"
    if wait_for 1200 "Beanstalk environment" "aws elasticbeanstalk describe-environments --environment-names $NAME --query 'Environments[0].Status' --output text" Ready; then
      status="$(aws elasticbeanstalk describe-environments --environment-names "$NAME" --query 'Environments[0].Health' --output text)"
      record "Elastic Beanstalk" WORKS "${stack}, single instance, health ${status} (payment simulator, ADR-048)"
    else fail "Elastic Beanstalk"; fi
  else fail "Elastic Beanstalk"; fi
  undo
}

# --- run -----------------------------------------------------------------------------------------------------------

# ONLY=leftovers lists anything named vigia-check-* that is still there (it deletes nothing).
check_leftovers() {
  local r
  for r in "$REGION" "$STANDBY"; do
    echo "== ${r}"
    aws rds describe-db-clusters --region "$r" --query "DBClusters[?starts_with(DBClusterIdentifier,'vigia-check')].DBClusterIdentifier" --output text
    aws rds describe-db-instances --region "$r" --query "DBInstances[?starts_with(DBInstanceIdentifier,'vigia-check')].DBInstanceIdentifier" --output text
    aws rds describe-global-clusters --region "$r" --query "GlobalClusters[?starts_with(GlobalClusterIdentifier,'vigia-check')].GlobalClusterIdentifier" --output text
    aws eks list-clusters --region "$r" --query "clusters[?starts_with(@,'vigia-check')]" --output text
    aws ecs list-clusters --region "$r" --query "clusterArns[?contains(@,'vigia-check')]" --output text
    aws elasticbeanstalk describe-environments --region "$r" --query "Environments[?starts_with(EnvironmentName,'vigia-check') && Status!='Terminated'].EnvironmentName" --output text
    aws dynamodb list-tables --region "$r" --query "TableNames[?starts_with(@,'vigia-check')]" --output text
  done
  aws s3api list-buckets --query "Buckets[?starts_with(Name,'vigia-check')].Name" --output text
}

# ONLY="api_gateway bedrock" runs just those checks (names without the check_ prefix).
QUICK=(quotas s3 dynamodb secrets_kms lambda_scheduler api_gateway iot rekognition bedrock data_lake ecr observability)
SLOW=(vpc_link fargate rds aurora eks beanstalk)
if [[ -n "${ONLY:-}" ]]; then
  read -r -a SELECTED <<< "$ONLY"
elif [[ $FULL == true ]]; then
  SELECTED=("${QUICK[@]}" "${SLOW[@]}")
else
  SELECTED=("${QUICK[@]}")
fi
for check in "${SELECTED[@]}"; do
  "check_${check}"
done

echo
echo "Done. Results in ${RESULTS}:"
column -s, -t < "$RESULTS" 2>/dev/null || cat "$RESULTS"
