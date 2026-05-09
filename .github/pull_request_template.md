<!--
Thanks for sending a pull request to GeoBlock! Please fill in the
sections below so a reviewer can understand and merge your change
without having to dig through the diff.
-->

## Summary

<!-- One to three sentences explaining what this PR changes and why. -->

## Related issues

<!-- e.g. Closes #12, fixes #34, or "none". -->

## Type of change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that changes runtime behavior or `config.yml` layout)
- [ ] Documentation only
- [ ] Refactor (no functional change)

## Testing

<!-- How did you verify this works? Be specific. -->

- [ ] `./gradlew build` passes locally
- [ ] Tested on a Paper development server with `./gradlew runServer`
- [ ] Manual scenarios run:
  - <!-- e.g. "Connected from a French IP with `mode: whitelist` + `countries: [FR]` and was allowed" -->

## Checklist

- [ ] Commit messages follow the [Conventional Commits](https://www.conventionalcommits.org/) style used in this repo (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:` ...)
- [ ] `CHANGELOG.md` has a new entry under an `## [Unreleased]` heading if user-visible behavior changed
- [ ] `README.md` and the inline comments in `config.yml` / `messages.yml` are up to date for any configuration change
- [ ] I have **not** bumped `gradle.properties`; the maintainer handles version bumps in a dedicated commit
