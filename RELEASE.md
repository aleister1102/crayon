# Release Process

This project uses a local script to build and publish releases to GitHub.

## Prerequisites
- **GitHub CLI (`gh`)**: Must be installed and authenticated (`gh auth login`).
- **Java 21**: Required to build the project.

## Steps to Create a Release

1.  **Update Version**:
    Open `build.gradle.kts` and update the version number:
    ```kotlin
    version = "0.2.0" // Example
    ```

2.  **Commit Changes**:
    ```bash
    git add build.gradle.kts
    git commit -m "chore: bump version to 0.2.0"
    ```

3.  **Run Release Script**:
    Run the `publish_release.sh` script from the project root:
    ```bash
    ./publish_release.sh
    ```

    This script will:
    - Clean and build the project locally.
    - Create a git tag (e.g., `v0.2.0`) if it doesn't exist.
    - Push the tag to GitHub.
    - Upload the generated JAR file to a new GitHub Release.

## Current version: 0.1.0
