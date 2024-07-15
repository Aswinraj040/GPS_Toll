Step 1 : Download the apk file which is available then run the server.py file which in the below link
Step 2 : https://github.com/Aswinraj040/GPS_TOLL_BASED_SIMULATION_PYTHON
Step 3 : Then run the GPS.py file which is in the above link

The GPS_Toll application is designed to automate the toll collection process by using live GPS coordinates transmitted from your mobile phone. The application communicates with a local server to send and receive necessary data, ensuring that the toll amount is dynamically calculated and debited from the user's account based on the vehicle's movement. This document provides a comprehensive overview of the application's functionality, from initial setup to continuous operation.

Application Setup and Initial Operation

1. Launching the Application
- Step 1: Open the GPS_Toll application on your device.
- Step 2: Enter your vehicle number in the designated input field.

2. Finding the Local Server IP Address
- Step 3: Press the "Get IP Address" button.
- Mechanism: The app scans the local network by sending requests to all possible 254 subnet IP addresses. This scan helps identify the local server's IP address.
  - **Technical Details**: The app sends HTTP requests to each IP address in the subnet (typically 192.168.0.1 to 192.168.0.254). The server responds to the correct request, revealing its IP address.

Continuous GPS Coordinate Transmission

3. Starting the Foreground Service
- Step 4: After identifying the server IP address, the app begins transmitting live GPS coordinates.
- Mechanism: A foreground service is initiated to send GPS coordinates every 1.5 seconds.
  - Technical Details: 
    - The app leverages Android's `LocationManager` to obtain precise GPS coordinates.
    - A `Service` runs in the foreground, ensuring continuous operation even when the app is closed.
    - The coordinates are sent as HTTP POST requests to the server's IP address at regular intervals (1.5 seconds).

4. Stopping the Service
- Step 5: To stop the coordinate transmission, open the app and press the "Stop" button.
- Mechanism: This action stops the foreground service, halting the transmission of GPS data.

Server-Side Operations

5. Setting Up the Local Server
- Step 6: Start the server developed using Python on the local network before initiating the app scan.
- Mechanism: 
  - The server listens for incoming HTTP POST requests containing GPS coordinates.
  - The server processes these coordinates and uses them for real-time tracking and toll calculation.

6. Real-Time Tracking and Toll Calculation
- Step 7: The server plots the vehicle's coordinates on a map using Matplotlib.
- Mechanism: 
  - As the vehicle moves, the marker on the map updates dynamically based on the received coordinates.
  - The server calculates toll amounts dynamically based on predefined toll zones and vehicle movement patterns.
  - The calculated toll amount is automatically debited from the user's account.

The GPS_Toll application streamlines toll collection by leveraging real-time GPS data and local network communication. By ensuring continuous GPS data transmission through a foreground service, the application remains functional even when closed. The server-side operations, including real-time tracking and dynamic toll calculation, provide a seamless user experience. 
