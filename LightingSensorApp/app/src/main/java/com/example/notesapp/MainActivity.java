package com.example.notesapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity {

    private final String apiUrl = "http://10.0.2.2:3000/ldr"; // API GET URL
    private TextView lightValueText;
    private TextView blueLightIndicator;
    private TextView greenLightIndicator;
    private TextView redLightIndicator;
    private TextView darkThresholdValue;
    private SeekBar darkThresholdSeekBar;
    private TextView blindingThresholdValue;
    private SeekBar blindingThresholdSeekBar;

    private int currentDarkThreshold = 300; // Default dark threshold
    private int currentBlindingThreshold = 700; // Default blinding threshold
    private int ldrValue = 0; // Current light sensor value

    private final Handler handler = new Handler();
    private final int updateInterval = 1000; // Update every second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        lightValueText = findViewById(R.id.lightValueText);
        blueLightIndicator = findViewById(R.id.blueLightIndicator);
        greenLightIndicator = findViewById(R.id.greenLightIndicator);
        redLightIndicator = findViewById(R.id.redLightIndicator);
        darkThresholdSeekBar = findViewById(R.id.darkThresholdSeekBar);
        darkThresholdValue = findViewById(R.id.darkThresholdValue);
        blindingThresholdSeekBar = findViewById(R.id.blindingThresholdSeekBar);
        blindingThresholdValue = findViewById(R.id.blindingThresholdValue);

        darkThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= currentBlindingThreshold) {
                    // Ensure dark threshold is always less than the blinding threshold
                    currentDarkThreshold = currentBlindingThreshold - 1;
                    darkThresholdSeekBar.setProgress(currentDarkThreshold);
                } else {
                    currentDarkThreshold = progress;
                }

                darkThresholdValue.setText("Value: " + currentDarkThreshold);

                // Send updated threshold values to the server
                sendThresholdsToArduino(currentDarkThreshold, currentBlindingThreshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        blindingThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= currentDarkThreshold) {
                    // Ensure blinding threshold is always greater than the dark threshold
                    currentBlindingThreshold = currentDarkThreshold + 1;
                    blindingThresholdSeekBar.setProgress(currentBlindingThreshold);
                } else {
                    currentBlindingThreshold = progress;
                }

                blindingThresholdValue.setText("Value: " + currentBlindingThreshold);

                // Send updated threshold values to the server
                sendThresholdsToArduino(currentDarkThreshold, currentBlindingThreshold);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        // Start fetching LDR data
        startFetchingLDRData();
        startFetchingThresholds();
    }

    private void startFetchingLDRData() {
        handler.post(fetchLDRDataRunnable);
    }

    private void stopFetchingLDRData() {
        handler.removeCallbacks(fetchLDRDataRunnable);
    }

    private final Runnable fetchLDRDataRunnable = new Runnable() {
        @Override
        public void run() {
            // Fetch LDR data
            new Thread(() -> {
                String result = fetchLDRData();
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        ldrValue = jsonObject.getInt("ldrValue");

                        // Update UI based on LDR value
                        runOnUiThread(() -> updateIndicators());
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error parsing LDR data", e);
                    }
                }
            }).start();

            // Schedule next fetch
            handler.postDelayed(this, updateInterval);
        }
    };

    private void updateIndicators() {
        // Update light value display
        lightValueText.setText("Current Light Value: " + ldrValue);

        // Update indicators based on thresholds
        if (ldrValue < currentDarkThreshold) {
            blueLightIndicator.setText("Blue: ON");
            greenLightIndicator.setText("Green: OFF");
            redLightIndicator.setText("Red: OFF");
        } else if (ldrValue >= currentDarkThreshold && ldrValue <= currentBlindingThreshold) {
            blueLightIndicator.setText("Blue: OFF");
            greenLightIndicator.setText("Green: ON");
            redLightIndicator.setText("Red: OFF");
        } else {
            blueLightIndicator.setText("Blue: OFF");
            greenLightIndicator.setText("Green: OFF");
            redLightIndicator.setText("Red: ON");
        }
    }

    private String fetchLDRData() {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error fetching LDR data", e);
        }
        return null;
    }

    private void sendThresholdsToArduino(int darkThreshold, int brightThreshold) {
        new Thread(() -> {
            try {
                // URL for the Node.js server
                URL url = new URL("http://10.0.2.2:3000/thresholds"); // Use the correct server address if different

                // Open a connection to the server
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Create JSON payload
                String jsonPayload = String.format("{\"dark\": %d, \"bright\": %d}", darkThreshold, brightThreshold);

                // Send data to the server
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Get the response code (to check if it was successful)
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("MainActivity", "Thresholds sent successfully.");
                } else {
                    Log.e("MainActivity", "Failed to send thresholds. Response code: " + responseCode);
                }

            } catch (Exception e) {
                Log.e("MainActivity", "Error sending thresholds to Arduino", e);
            }
        }).start();
    }

    private void startFetchingThresholds() {
        handler.post(fetchThresholdsRunnable);
    }

    private final Runnable fetchThresholdsRunnable = new Runnable() {
        @Override
        public void run() {
            // Fetch thresholds
            new Thread(() -> {
                String result = fetchThresholds();
                if (result != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        int serverDarkThreshold = jsonObject.getJSONObject("thresholds").getInt("dark");
                        int serverBlindingThreshold = jsonObject.getJSONObject("thresholds").getInt("bright");

                        // Update thresholds and SeekBars if they have changed
                        if (currentDarkThreshold != serverDarkThreshold || currentBlindingThreshold != serverBlindingThreshold) {
                            currentDarkThreshold = serverDarkThreshold;
                            currentBlindingThreshold = serverBlindingThreshold;

                            runOnUiThread(() -> {
                                darkThresholdSeekBar.setProgress(currentDarkThreshold);
                                blindingThresholdSeekBar.setProgress(currentBlindingThreshold);
                                darkThresholdValue.setText("Value: " + currentDarkThreshold);
                                blindingThresholdValue.setText("Value: " + currentBlindingThreshold);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error parsing thresholds data", e);
                    }
                }
            }).start();

            // Schedule next fetch
            handler.postDelayed(this, updateInterval);
        }
    };

    private String fetchThresholds() {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error fetching thresholds", e);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFetchingLDRData();
        handler.removeCallbacks(fetchThresholdsRunnable); // Stop fetching thresholds
    }
}
