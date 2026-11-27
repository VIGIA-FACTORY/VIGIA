// Stage 0 of the pipeline: Conventional Commits with the Jira key (05.01 §2.2, D-79).
//   feat(s-01): SCRUM-82 register community members
// Merge commits made by GitHub are ignored by commitlint by default. The body (what, why, how tested) is
// reviewed in the pull request: commitlint reads lines such as "Why: ..." as trailers, not as body.
//
// The Jira key is checked with a local rule on the subject. The built-in references-empty rule is not used:
// its parser treats words such as "resolved", "fixes" or "closes" in a subject as an action that must be
// followed by the key, so "SCRUM-62 lock the resolved dependencies" was reported as having no reference.
const JIRA_KEY = /^SCRUM-\d+ /;

export default {
  // Dependency updates from Renovate (chore(deps): ...) carry no Jira key (05.01 §3.3).
  ignores: [(message) => message.startsWith('chore(deps)')],
  extends: ['@commitlint/config-conventional'],
  plugins: [
    {
      rules: {
        'subject-jira-key': ({ subject }) => [
          JIRA_KEY.test(subject ?? ''),
          'the subject must start with the Jira key, for example "SCRUM-82 register community members"',
        ],
      },
    },
  ],
  rules: {
    'subject-jira-key': [2, 'always'],
    'body-max-line-length': [0],
    'subject-case': [0],
    'header-max-length': [2, 'always', 100],
  },
};
