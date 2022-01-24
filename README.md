# Minimal Hitting Set

**NP-Complete Problem**: Given a ground set X of elements and also a grouping collection C of subsets available in X and an integer k, the task is to find the smallest subset of X, such that the smallest subset, H hits every set comprised in C. 
This implies that the intersection of H and S is null for every (sub)set S belonging to C, with size ≤ k.

## Requirements
- Java 8
- Maven (recommended version 3.6.3)
- IDE (e.g. IntelliJ IDEA, Eclipse)

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

### Import Project (IntelliJ IDEA + Maven)
1. Open IntelliJ IDEA and click on ```File > New > Project from Existing Source...```
2. Select ```minimal-hitting-set``` directory and click on ```Open``` button
3. Install all dependencies and create the executable **_.jar_** by using the _Maven tool window_: click on ```Run Maven Build``` ([Troubleshooting common Maven issues](https://www.jetbrains.com/help/idea/troubleshooting-common-maven-issues.html))

### Execution
1. Open a ```Terminal``` window
2. `cd path/to/project/minimal-hitting-set/target/`
 
### Syntax
```console
java -jar minimal-hitting-set-maven-project-1.0.jar 
  Options:
    -d, -dir, --directory
      Absolute path of the directory that contains benchmark files
      Default: <empty string>
    -h, --help
      Print this help message and exit
    -in, --input-file
      Absolute path of the input file .matrix
      Default: <empty string>
    -out, --output-path
      Absolute path of the output file (.out) with report information
      Default: /Users/matteorubagotti/output
    -pe, -pre, --pre-elaboration
      Compute the Pre-Elaboration before execute MBase procedure
      Default: false
  * -t, --timeout
      Maximum time limit in seconds (s)
      Default: 0
    -v, --verbose
      Print additional information on standard output
      Default: false
```
_Note: options preceded by an asterisk are **required**._

##### Example (MacOS/UNIX)
The command below starts an execution with pre-elaboration, timeout of 60 seconds and the input matrix is stored at the following path: `/Users/user/benchmarks/example.matrix`
```console
java -jar minimal-hitting-set-maven-project-1.0.jar -pe -t 60 -in /Users/user/benchmarks/example.matrix 
```

### Benchmarks
The input benchmark file extension is `.matrix`.
#### Example
This **incidence** matrix has 3 rows and 5 columns. 
Each column represents an element of the domain (X) while a row is a set of elements (C). 
For example the first element (`column_1`) belongs to the first and second set (`row_1` and `row_2`).
```
;;; This is a comment
;;; Additional information
1 0 1 0 1 -
1 1 1 1 1 -
0 1 0 0 1 -
```



