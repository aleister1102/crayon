# Crayon

![Build and Release](https://github.com/aleister1102/crayon/workflows/Build%20and%20Release/badge.svg)

A Burp Suite extension that automatically colorizes request/response entries in the Proxy history, Target, Logger, Intruder, and WebSocket traffic, making it easier to spot interesting items at a glance.

## Features

Crayon uses a set of predefined and configurable rules to apply highlight colors:

**HTTP Responses (by Status Code):**
*   **5xx Server Error:** Red
*   **4xx Client Error:** Orange
*   **3xx Redirection:** Yellow

**HTTP Responses (by Content-Type for 2xx Status):**
*   **JSON:** Green (Configurable)
*   **XML:** Blue (Configurable)
*   **HTML:** Cyan (Configurable)

**WebSocket Messages:**
*   **Incoming (Server to Client):** Green (Configurable)
*   **Outgoing (Client to Server):** Yellow (Configurable)

**Context Menu:**
*   Right-click on any request/response to access the "Crayon" menu
*   **Auto-highlight:** Apply highlighting rules to selected items
*   **Set color:** Manually set any highlight color
*   **Clear highlight:** Remove highlighting from selected items

**Site Map Context Menu (additional options):**
*   **Auto-highlight URL prefix:** Apply rules to all items sharing the same URL prefix
*   **Set color for URL prefix:** Apply a color to all items under the same path
*   **Clear highlight for URL prefix:** Remove highlighting from all items under the same path

## Supported Tools

*   Proxy
*   Target
*   Logger
*   Intruder
*   WebSocket History

## Configuration

You can customize the colors for different content types, status codes, WebSocket directions, and enable/disable logging via the "Crayon Settings" panel in the Burp Suite **Settings** dialog.

## Installation

### Download from Releases
1. Go to the [Releases](https://github.com/aleister1102/crayon/releases) page
2. Download the latest `Crayon-X.X.X.jar` file
3. In Burp Suite, go to **Extensions > Add**
4. Select the downloaded JAR file

### Build from Source
Before you begin, make sure that your project's JDK is set to version "21" or higher.

To build the JAR file, run the following command in the root directory of this project:

*   For UNIX-based systems: `./gradlew jar`
*   For Windows systems: `gradlew jar`

If successful, the JAR file is saved to `build/libs/Crayon.jar`.

## Loading the JAR file into Burp

To load the JAR file into Burp:

1.  In Burp, go to **Extensions > Installed**.
2.  Click **Add**.
3.  Under **Extension details**, click **Select file**.
4.  Select the `Crayon.jar` file you just built, then click **Open**.
5.  Click **Next**. The extension is loaded into Burp.
6.  Click **Close**.

Your extension is now loaded. You can configure it under the main Burp **Settings** window.

### Reloading the JAR file in Burp

If you make changes to the code, you must rebuild the JAR file and reload your extension in Burp for the changes to take effect.

To rebuild the JAR file, follow the steps for [building from source](#build-from-source).

## Creating Releases

This project automatically builds and releases based on your git actions:

### Automatic Build on Push
- **Push to master**: Automatically builds and uploads JAR artifacts (available for 90 days)
- **Push tag**: Automatically builds and creates a GitHub release with JAR attached

### To Create a New Release:

1. **Update version in `build.gradle.kts`**:
   ```kotlin
   version = "1.0.1"  // Change this line
   ```

2. **Commit and push the version change**:
   ```bash
   git add build.gradle.kts
   git commit -m "Bump version to 1.0.1"
   git push origin master
   ```

3. **Create and push a version tag**:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

4. **GitHub Actions will automatically**:
   - Build the JAR file
   - Create a GitHub release
   - Attach the JAR file to the release

### Workflow Triggers:
- **Push to master**: Build + Upload artifacts
- **Push version tag (v*)**: Build + Create release
- **Pull requests**: Build and test only

## CI/CD

This project uses GitHub Actions with automated build and release:

- **Build and Release Workflow** (`.github/workflows/release.yml`): 
  - Builds and tests on every push and pull request
  - Uploads JAR artifacts when pushing to master (90 days retention)
  - Creates automatic releases when pushing version tags

### Simple workflow:
1. **Development**: Push code to master → Auto build + artifact upload
2. **Release**: Push version tag → Auto build + GitHub release creation
3. **Testing**: Create PR → Build and test only

To rebuild the JAR file, follow the steps for [building from source](#build-from-source).

To quickly reload your extension in Burp:

1.  In Burp, go to **Extensions > Installed**.
2.  Hold `Ctrl` or `⌘`, and double-click the **Loaded** checkbox next to your extension to unload and reload it.

## Contributing

Feel free to open issues or pull requests if you have suggestions or find bugs.