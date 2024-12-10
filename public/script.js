document.addEventListener("DOMContentLoaded", () => {
  const ldrDisplay = document.getElementById("ldrValue");
  const darkSlider = document.getElementById("darkSlider");
  const darkValue = document.getElementById("darkValue");
  const brightSlider = document.getElementById("brightSlider");
  const brightValue = document.getElementById("brightValue");
  const blueLed = document.getElementById("blueLed");
  const greenLed = document.getElementById("greenLed");
  const redLed = document.getElementById("redLed");
  const ledStatusText = document.getElementById("ledStatusText");

  // Helper function to send thresholds to the server
  const sendThresholdsToServer = () => {
    const thresholds = {
      dark: parseInt(darkSlider.value, 10),
      bright: parseInt(brightSlider.value, 10)
    };

    fetch("http://localhost:3000/thresholds", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(thresholds)
    })
      .then(response => response.json())
      .then(data => {
        if (data.success) {
          console.log("Thresholds sent to server successfully");
        } else {
          console.error("Error saving thresholds on the server");
        }
      })
      .catch(err => {
        console.error("Error:", err);
      });
  };

  // Ensure dark threshold doesn't exceed bright threshold
  darkSlider.addEventListener("input", () => {
    if (parseInt(darkSlider.value, 10) >= parseInt(brightSlider.value, 10)) {
      darkSlider.value = brightSlider.value - 1;
    }
    darkValue.textContent = darkSlider.value;
  });

  brightSlider.addEventListener("input", () => {
    if (parseInt(brightSlider.value, 10) <= parseInt(darkSlider.value, 10)) {
      brightSlider.value = parseInt(darkSlider.value, 10) + 1;
    }
    brightValue.textContent = brightSlider.value;
  });

  // Send thresholds when slider is released
  darkSlider.addEventListener("mouseup", sendThresholdsToServer);
  brightSlider.addEventListener("mouseup", sendThresholdsToServer);

  // Establish WebSocket connection
  const socket = new WebSocket("ws://localhost:3000");

  socket.onopen = () => {
    console.log("WebSocket connected");
  };

  socket.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.ldrValue !== undefined) {
    ldrDisplay.textContent = `Apšvietimo lygis: ${data.ldrValue}`;
    
    // Update LED statuses
    blueLed.classList.remove("on");
    greenLed.classList.remove("on");
    redLed.classList.remove("on");

    if (data.ldrValue < parseInt(darkSlider.value, 10)) {
      blueLed.classList.add("on");
      ledStatusText.textContent = "Mėlyna šviesa: Per tamsu";
    } else if (data.ldrValue > parseInt(brightSlider.value, 10)) {
      redLed.classList.add("on");
      ledStatusText.textContent = "Raudona šviesa: Per šviesu";
    } else {
      greenLed.classList.add("on");
      ledStatusText.textContent = "Žalia šviesa: Optimalus apšvietimas";
    }
  }

  if (data.thresholds) {
    // Update sliders and displayed threshold values
    darkSlider.value = data.thresholds.dark;
    darkValue.textContent = data.thresholds.dark;
    brightSlider.value = data.thresholds.bright;
    brightValue.textContent = data.thresholds.bright;

    console.log("Thresholds updated from server:", data.thresholds);
  }
};


  socket.onclose = () => {
    console.log("WebSocket disconnected");
  };

  // Fetch current thresholds from server
  fetch("http://localhost:3000/ldr")
    .then(response => response.json())
    .then(data => {
      darkSlider.value = data.thresholds.dark;
      darkValue.textContent = data.thresholds.dark;
      brightSlider.value = data.thresholds.bright;
      brightValue.textContent = data.thresholds.bright;
    })
    .catch(err => {
      console.error("Error fetching thresholds:", err);
    });
});
