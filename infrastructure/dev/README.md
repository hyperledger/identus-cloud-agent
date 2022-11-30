# Developing locally

This folder contains scripts and a docker-compose file to run the Atala `building-block` stack for development purposes.

All components are built at the current repository version and published locally. 

> If you want to run a local instance without compilation - for end-user use - please see the `infrastructure/local` folder. 

**Running using the scripts in this directory does not create a production-ready or secure environment. It is designed to allow easy development and should not be used to run a production instance**

Please ensure you have set the `ATALA_GITHUB_TOKEN` and `GITHUB_TOKEN` environment variable. These both need to be set to the same value. 

The value of this variable must be a Github token generated with the  `read:packages` permission set on the `building-block` repository.

> Replace `YOUR_TOKEN_HERE` with your Github token

```
export GITHUB_TOKEN=YOUR_TOKEN_HERE
export ATALA_GITHUB_TOKEN=$GITHUB_TOKEN
```

Please ensure you have logged into the ATALA IOHK Docker Registry using the following command [once the `GITHUB_TOKEN` is set]

> Replace `YOUR_USERNAME_HERE` with your Github username

```
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_USERNAME_HERE} --password-stdin
```

## Scripts

| Name     | Purpose                                                                                              | Notes                                                                                      |
| -------- | ---------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| build.sh | Compile and Publish libraries and docker images locally                                              | Requires sbt and Docker installed                                                          |
| run.sh   | Retrieves versions from local components and brings up a docker-compose stack using built components | Does not build images or libraries                                                         |
| clean.sh | Clean local build cache                                                                              | Runs sbt clean;cleanFiles to clear local cache / state from build directories              |
| full.sh  | Runs `build.sh` followed by `run.sh`                                                                 | Use this to build and run the stack. Sometimes quite slow if not rebuilding all components |
