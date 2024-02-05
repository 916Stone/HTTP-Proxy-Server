# Proxy Server Setup Instructions

Follow these steps to set up and run the proxy server:

1. **Navigate to the Root Folder:**
   - Ensure you are in the `Proxy` folder, which is the root folder for the proxy server.

2. **Locate the Launch File:**
   - Find the `Launch.bat` file within the `Proxy` folder.
   - Keep the batch file in this folder for proper operation.

3. **Port Configuration:**
   - The default port number is set to `8080`.
   - Configure your browser to use port `8080`.
   - If you need to use a different port, you can modify the port setting in the batch file.

4. **Running the Server:**
   - You may need to run `Launch.bat` as an administrator.
   - After launching, a console window should appear displaying messages indicating that the server is active and showing the port number.

5. **Important Exit Instructions:**
   - To exit the program, use `CTRL + C` in the console.
   - **Warning:** Closing the window directly without using `CTRL + C` may not trigger a graceful shutdown, resulting in the cache map not being saved properly.

6. **Alternative Setup:**
   - If the above instructions do not work, you can open the `Proxy` folder as a Java project in your IDE and run it from there.
   - The setup was tested with IntelliJ IDEA and may not work identically in other IDEs.

7. **Demo Video:**
   - For additional help and a visual guide, watch the [demo video](https://drive.google.com/file/d/1aeNFKcALR6J0-p0S-k3G-9HrqVYv4KMM/view?usp=sharing).
