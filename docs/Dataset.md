# Dataset

## Dataset format

Currently, dataset consists on the root level of three objects: `config.yaml` file, `projects/` folder and `validate.sh`
script.

### config.yaml

`config.yaml` is a file that contains all necessary information about this dataset.
On the root level there is only one field, `projects`, which is an array of all dataset projects.

#### Dataset project

Dataset project is an object with the following fields:

- `path` — path to the project directory. Should be relative and starts from `projects/`
- `modules` — information about number of project's modules. For now, it could be either `single` or `multiple`
- `buildSystem` — build system information, see below
- `reproduce` — a relative path from config's directory to the script that runs the compilation and reproduces the issue
- `kotlinVersion` — a version of the Kotlin that is used to reproduce the issue
- `extra` — an object that represents extra and not necessary information. For now, it's:
    - `tags` — an array of string with helpful tags that associate with the project. Could be any strings you want
    - `issue` — a ticket's number in Kotlin's YouTrack

#### Build system

Build system is the object that represents the project's build system. For now, it contains two fields:

- `type` — a name of the build system. E.g. `gradle` or `maven`
- `version` — the used build system's version

### projects/

That folder contains all the projects, described in the `config.yaml`. Some projects may contain git, others don't.

### validate.sh

`validate.sh` is a simple script that checks that all the `reproduce` scripts are valid (build failed on every
script).  
More detailed checks aren’t done yet due to complexity of the different problems.

# Example yaml
```yaml
projects:
  - name: example
    path: projects/example
    reproduce: projects/example/reproduce.sh
    buildSystem:
      name: gradle
      version: 8.0
    kotlinVersion: 2.0.20
    extra:
      tags: ["android", "mpp", "pcla", "rust"]
      issue: KT-56630
```