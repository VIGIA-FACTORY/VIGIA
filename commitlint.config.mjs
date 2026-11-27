// Stage 0 of the pipeline: Conventional Commits with the Jira key (05.01 §2.2, D-79).
//   feat(s-01): SCRUM-82 register community members
// Merge commits made by GitHub are ignored by commitlint by default. The body (what, why, how tested) is
// reviewed in the pull request: commitlint reads lines such as "Why: ..." as trailers, not as body.
export default {
  extends: ['@commitlint/config-conventional'],
  parserPreset: {
    parserOpts: {
      issuePrefixes: ['SCRUM-'],
    },
  },
  rules: {
    'references-empty': [2, 'never'],
    'body-max-line-length': [0],
    'subject-case': [0],
    'header-max-length': [2, 'always', 100],
  },
};
