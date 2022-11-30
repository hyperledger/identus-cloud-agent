# Running locally

This folder contains scripts to run the Atala `building-block` stack for end-user usage - without building any local components.

All images will be pulled from a remote repository and the `.env` file controls the versions of these images.

> If you want to run the `building-block` stack for development purposes - please see the `infrastructure/dev`  folder. 

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

## Example usage

To run the Atala `building-block` stack - execute the `run.sh` script. This can be run from within the directory or at the root of the source code project:

`./infrastructure/local/run.sh` 

## Scripts

| Name   | Purpose                              | Notes                                                                    |
| ------ | ------------------------------------ | ------------------------------------------------------------------------ |
| run.sh | Run the Atala `building-block` stack | Runs using docker-compose and versions are controlled by the `.env` file |
