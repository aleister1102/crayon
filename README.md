# Crayon

A Burp Suite extension that automatically colorizes request/response entries in the Proxy history and other tools, making it easier to spot interesting items at a glance.

## Features

Crayon uses a set of predefined and configurable rules to apply highlight colors:

**Requests:**
*   `POST`, `PUT`, `PATCH`: Yellow
*   `DELETE`: Red

**Responses (by Status Code):**
*   **5xx Server Error:** Red
*   **4xx Client Error:** Orange
*   **3xx Redirection:** Yellow

**Responses (by Content-Type for 2xx Status):**
*   **JSON:** Green (Configurable)
*   **XML:** Blue (Configurable)
*   **HTML:** Magenta (Configurable)

## Configuration

You can customize the colors for different content types and enable/disable logging via the "Crayon Settings" panel in the Burp Suite **Settings** dialog.

## Building the extension

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

To rebuild the JAR file, follow the steps for [building the JAR file](#building-the-jar-file).

To quickly reload your extension in Burp:

1.  In Burp, go to **Extensions > Installed**.
2.  Hold `Ctrl` or `⌘`, and double-click the **Loaded** checkbox next to your extension to unload and reload it.

## Contributing

Feel free to open issues or pull requests if you have suggestions or find bugs.