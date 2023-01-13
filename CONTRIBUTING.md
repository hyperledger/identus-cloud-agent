# Contributing to Atala PRISM

We would love for you to contribute to Atala PRISM and help make it even better than it is today!
As a contributor, here are the guidelines we would like you to follow:

- [Pull Requests](#pull-requests)
- [Commit Message Guidelines](#commits)
- [Release process](#releases)
- Software development lifecycle (TBD)


## Pull Requests

### Submitting a Pull Request

**All code changes to the `main` branch must go through Pull Requests (PRs)**

To create a Pull Request, follow the [Official GitHub PR guidelines](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request)

> Use `Create draft pull Request` option to create work-in-progress (WIP) PRs.
> Remove draft state when PR is ready for review.

Each pull request should be small enough to be reviewed in less than 30 minutes.
Otherwise, break the PR into several ones.

**NOTE**: It's important to create new commits when addressing the reviewer comments (instead of `git commit --amend`), so that we know what exactly changed.

### Pull Request naming

Pull Requests are expected to be named like `[ATL-XXXX]: short description` (ticket id as prefix with a short but understandable description).

> Please note, PR title should not be a part of the `main` branch commit history. All commits in `main` should correspond to the [Commit Message Guidelines](#commits).

### Pull Request checklist

All Pull Requests will get a checklist that everyone is expected to follow, failing to do so might delay people getting to review it.

Check the [PR checklist](.github/PULL_REQUEST_TEMPLATE.md) for more info.

### Pull request CI checks

All pull requests will be subject to the following checks and actions:

| Name                  | Type   | Description                                                                                                                                                                                                                                                                           |
|-----------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Linter                | Check  | Powered by megalinter - any changed source will be checked against configured linting rules (configured in .mega-linter.yml at root of repository). No automatic fixes will be generated. If the check fails, the author of the pull request must fix issues and update pull request. |
| Pull request labeller | Action | Pull Request will be labelled based upon directory structure                                                                                                                                                                                                                          |


### Pull Request review

The team `input-output-hk/atala` will be automatically set as reviewer when the PR is created, so that 2 reviewers from the team get assigned.

After that, you can add any other reviewer that should review the PR.

At least 1 approval is mandatory to merge a PR.

Check [CODEOWNERS](.github/CODEOWNERS) for more info about main owners/reviewers of each part of the repository.

### Pull Request merging

You are responsible to merge your PRs when they are ready, before that, ensure that your branch has all the changes from the base branch (like `main`).

> Avoid merge commits, use rebase instead, a simple way is to just run `git config --global pull.rebase true` on each repository or run `git config --global pull.rebase true` once to get the effect on all repositories.

**NOTE** If a PR breaks `main` (usually identified by CI checks failure), we shouldn't merge it but keep it opened until we are able to merge it without breaking `main`, this applies to experimental or controversial changes. NOT merge experimental stuff, keep it in a PR.

Once the Pull Request is ready to be merged, you will have 2 options:
1. `Squash and Merge`
2. `Rebase and Merge`

#### Squash and merge

`Squash and Merge` feature from Github combines all the PR commits into a single one.

You are expected to review the auto-generated message and update it as necessary to get a decent commit message that fully corresponds to the [Commit Message Guidelines](#commits).

Use this option when all commits you made in the PR can be combined into one.

If this is not the case, use `Rebase and Merge` option.

#### Rebase and Merge

`Rebase and Merge` feature from Github attached all commits from the PR branch on top of the base branch.

You are expected to make sure that **all the commits** in the PR are corresponding to the [Commit Message Guidelines](#commits).

Use this option when the update you're working on contains several commits that should be merged to the `main` branch of the repository.

## Commits

The following general rules are applied to all commits:

- Must be a small incremental change
- Must be signed and verified by GitHub

> Follow [signing commits](docs/guides/signing-commits.md) guide to set up GPG keys to correctly sign your commits.

### <a name="commit"></a> Commits Message Format

We have very precise rules over how our Git commit messages must be formatted.
This format leads to **easier to read commit history**.

This format is based on the [Conventional Commits Specification](https://www.conventionalcommits.org/en/v1.0.0/#summary).

Each commit message consists of a mandatory **header**, an optional **body**, and an optional **footer**.

```text
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The `header` is mandatory and must conform to the [Commit Message Header](#commit-message-header) format.

The `body` is optional.
When the body is present it must be at least 20 characters long and must conform to the [Commit Message Body](#commit-message-body) format.

The `footer` is optional. The [Commit Message Footer](#commit-message-footer) format describes what the footer is used for and the structure it must have.

#### Commit Message Header

```
<type>(<scope>): <short summary>
  │       │             │
  │       │             └─⫸ Summary in present tense. Not capitalized. No period at the end.
  │       │
  │       └─⫸ Commit Scope: iris|apollo|castor|pollux|mercury|pluto|athena|pistis|atlas|agent
  │
  └─⫸ Commit Type: build|ci|docs|feat|fix|perf|refactor|test

The <type> and <summary> fields are mandatory, the (<scope>) field is optional.
```

The `<type>` and `<summary>` fields are mandatory, the `(<scope>)` field is optional.

##### Type

Must be one of the following:

* **build**: Changes that affect the build system or external dependencies
* **ci**: Changes to CI configuration files and scripts
* **docs**: Documentation only changes
* **feat**: A new feature
* **fix**: A bug fix
* **perf**: A code change that improves performance
* **refactor**: A code change that neither fixes a bug nor adds a feature
* **test**: Adding missing tests or correcting existing tests

##### Scope

The scope should be the name of the affected module or building block (as perceived by the person reading the changelog generated from commit messages).

The following is the list of supported scopes:

* `iris`
* `apollo`
* `castor`
* `pollux`
* `mercury`
* `pluto`
* `athena`
* `pistis`
* `atlas`
* `prism-agent`
* `prism-node`
* `shared`
* `infra`

##### Summary

Use the summary field to provide a succinct description of the change:

* use the imperative, present tense: "change" not "changed" nor "changes"
* don't capitalize the first letter
* no dot (.) at the end

#### Commit Message Body

Just as in the summary, use the imperative, present tense: "fix" not "fixed" nor "fixes".

Explain the motivation for the change in the commit message body. This commit message should explain why you are making the change. You can include a comparison of the previous behavior with the new behavior in order to illustrate the impact of the change.

#### Commit Message Footer

The footer can contain information about breaking changes and deprecations and is also the place to reference GitHub issues, Jira tickets, and other PRs that this commit closes or is related to. For example:

```
BREAKING CHANGE: <breaking change summary>
<BLANK LINE>
<breaking change description + migration instructions>
<BLANK LINE>
<BLANK LINE>
Fixes ATL-<ticket number>
```

or

```
DEPRECATED: <what is deprecated>
<BLANK LINE>
<deprecation description + recommended update path>
<BLANK LINE>
<BLANK LINE>
Related to ATL-<ticket number>
```

Breaking Change section should start with the phrase "BREAKING CHANGE: " followed by a summary of the breaking change, a blank line, and a detailed description of the breaking change that also includes migration instructions.

Similarly, a Deprecation section should start with "DEPRECATED: " followed by a short description of what is deprecated, a blank line, and a detailed description of the deprecation that also mentions the recommended update path.

### Revert commits

If the commit reverts a previous commit, it should begin with `revert:`, followed by the header of the reverted commit.

The content of the commit message body should contain:

* information about the SHA of the commit being reverted in the following format: This reverts commit <SHA>,
* a clear description of the reason for reverting the commit message.

### Local environment: pre-commit hooks

[Conventional pre-commit hook](https://github.com/compilerla/conventional-pre-commit)
can be used to check that all commits correspond to the Conventional Commits Specification.

Initialization:
* Make sure pre-commit is [installed](https://pre-commit.com/#install).
* Run: `pre-commit install --hook-type commit-msg`

> Please note: even if local `pre-commit` set-up is not in use
> all violations will be found during CI jobs execution for a PR

## Releases

Release process of all Atala PRISM components is automated via [Semantic Release](https://github.com/semantic-release/semantic-release) tool
with help of additional `gradle` or `sbt` library specific plugins.

[Semantic Release](https://github.com/semantic-release/semantic-release) automates the whole package release workflow including: determining the next version number, generating the release notes, and publishing the package.

This removes the immediate connection between human emotions and version numbers, strictly following the Semantic Versioning specification and communicating the impact of changes to consumers.

General release steps for each building block include:
1. Analyze and filter conventional commits for each component to automatically identify the next semantic version for it
2. Generate release notes based on changes in the component
3. Build, test, and publish packages
4. Update `CHANGELOG.md`
5. Create tag and release

For more specific details about the release process of each component, please, check `README.md` for each building block in the corresponding directory.

### Prereleases

Special branches named `prerelease/<building-block>` exist to generate pre-releases of libraries.

If a release is triggered from a `prerelease/*` branch, then a special pre-release version of libraries will be published with additional `prerelease/<building-block>` suffix that can be used to test a product before the publication of a main version.

### Triggering a release

Special [release workflow](.github/workflows/release.yml) exists to start release generation for a component / building block.

It has the following parameters:
* `release-component`: a component to release
* `release-branch`: branch to release from

By default, `release-branch` is always `main`.

To trigger a new release generation, follow the next steps:
1. Go to `Actions` tab in Github for `atala-prism-building-block` repository
2. Choose `Release` workflow on the left panel of all available workflows
3. Choose `Run workflow` on the pop-up
4. Specify input parameters - component and branch
5. Press `Run workflow` green button

If all release conditions are met, then a new release will be published.

> Please note: release won't be published if branch does not correspond to `main` or `prerelease/*`, or if there were no changes for a component since the latest release

To check the next version and dry-run on local environment, use the following commands:
```shell
cd <component_dir>
npm install
npx semantic-release -e semantic-release-monorepo --dry-run
```
