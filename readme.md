# scalac tech task

## Introduction

Goal is to create an endpoint that given the name of the organization will return a list of contributors sorted by the number of contributions.

## Available Endpoints

There are three implementations of receiving all concrete organization's contributors endpoints:  
**GET** localhost:8080/orgs/org_name/contributors - receiving only first at most 100 of first page  
**GET** localhost:8080/v2/orgs/org_name/contributors - recursive algorithm  
**GET** localhost:8080/v3/orgs/org_name/contributors - tail recursive algorithm, but using await result  

Additional endpoints for testing purposes:  
**GET** localhost:8080/orgs/org_name/repos- receiving first 100 repositories of concrete organization  
**GET** localhost:8080/v2/orgs/org_name/repos- receiving all repositories of concrete organization recursively  
**GET** localhost:8080/v3/orgs/org_name/repos- receiving all repositories of concrete organization tail recursively  

**GET** localhost:8080/repos/org_name/repo_name/contributors- receiving only first at most 100 contributors of first page  
**GET** localhost:8080/v2/repos/org_name/repo_name/contributors- receiving all contributors of concrete organization's repository recursively  
**GET** localhost:8080/v3/repos/org_name/repo_name/contributors- receiving all contributors of concrete organization's repository tail recursively  

**GET** localhost:8080/healthcheck

## Installation and Interaction

1. set environment variable GH_TOKEN=your_token  
2. launch using `sbt run` command  
3. interact (**GET** localhost:8080/v3/orgs/org_name/contributors example):  
   curl --location --request GET 'localhost:8080/v3/orgs/scalaconsultants/contributors'  
   or without caching:  
   curl --location --request GET 'localhost:8080/v3/orgs/scalaconsultants/contributors' \
   --header 'Cache-Control: no-cache'