# Minimal Hitting Set

**NP-Complete Problem**: Given a ground set X of elements and also a grouping collection C of subsets available in X and an integer k, the task is to find the smallest subset of X, such that the smallest subset, H hits every set comprised in C. 
This implies that the intersection of H and S is null for every set S belonging to C, with size ≤ k.

## Requirements
- Java 8
- Maven (Recommended version 3.6.3)
- IDE (e.g. IntelliJ IDEA)

## Installation & Run
### Git Clone
**Attention**: Support for password authentication was removed on August 13, 2021. *Please use a **personal access token** (**PAT**) instead*.
<a href="https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token#creating-a-token">Creating a personal access token</a>

```console
$ git clone https://github.com/MatteoRubagotti/minimal-hitting-set.git
$ Username for 'https://github.com': username
$ Password for 'https://username@github.com': personalAccessToken
```
where ```username``` is your GitHub username.

### Import Project (IntelliJ IDEA)
1. Open IntelliJ IDEA and click on ```File > New > Project from Existing Source...```
2. Select ```minimal-hitting-set``` directory and click on ```Open``` button
3. Install all dependencies and create the executable **_.jar_** by using the _Maven tool window_: click on ```Lifecycle > install``` ([Troubleshooting common Maven issues](https://www.jetbrains.com/help/idea/troubleshooting-common-maven-issues.html))

### Execution
1. Open a ```Terminal``` window
2. `cd pathToProject/minimal-hitting-set/target/`
 
#### Syntax
```console
java -jar minimal-hitting-set-maven-project-1.0.jar -in pathToInputMatrix
```




