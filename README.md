# Indoor Localization Using Wireless Technology for 2D Positioning & AR Navigation

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)  
![Status](https://img.shields.io/badge/status-in%20progress-orange)  
![Tech](https://img.shields.io/badge/Tech-WiFi%20%7C%20BLE%20%7C%20Unity%20ARCore-green)

---

## 📌 Overview
This research project focuses on **indoor localization** using **wireless technologies** to achieve accurate **two-dimensional positioning** and provide **real-time AR navigation** between localized points.  
The solution combines **Wi-Fi floor detection**, **BLE trilateration**, and **RSSI analysis**, integrated with **A\* pathfinding** and **Unity ARCore overlays** for seamless indoor navigation.

---

## 🎯 Objectives
- Develop a **low-cost hybrid indoor localization model** using Wi-Fi & BLE.  
- Implement **RSSI-based trilateration** for X–Y coordinate estimation.  
- Apply **A\* algorithm** for pathfinding between two points.  
- Build a **mobile AR navigation system** with Unity & ARCore.  
- Evaluate accuracy, usability, and system limitations in real-world environments.

---

## 🏗️ System Architecture
```
[ Smartphone ]
      |
      | (Wi-Fi & BLE signals)
      ↓
[ Localization Engine ]
   • Wi-Fi floor detection
   • BLE trilateration (X, Y)
   • RSSI smoothing
      ↓
[ Pathfinding ]
   • A* algorithm
      ↓
[ AR Navigation Layer ]
   • Unity ARCore overlay
   • Camera-based guidance
```

---

## ⚙️ Technologies & Tools
- **Wireless Technologies:** Wi-Fi, BLE Beacons (Eddystone/iBeacon)  
- **Localization Methods:** RSSI-based Trilateration, Signal Filtering  
- **Algorithms:** A* Pathfinding, Heuristic Functions  
- **AR Platform:** Unity 3D, ARCore SDK  
- **Mobile Development:** Android Studio (Java/Kotlin)  
- **Other Tools:** GitHub, LaTeX (for documentation), MATLAB/Python (for testing accuracy)  

---

## 🔬 Methodology
1. **Initialization** – Floor detection using Wi-Fi APs or manual selection.  
2. **Localization** – Estimate position with BLE trilateration (distance from RSSI).  
3. **Pathfinding** – Apply A* algorithm to compute the shortest path.  
4. **AR Navigation** – Render the path with Unity ARCore overlay on live camera feed.  
5. **Evaluation** – Measure accuracy, latency, and user experience in testbed.  

---

## 📐 Equations

### RSSI-based Distance Estimation
```
d = 10 ^ ((TxPower - RSSI) / (10 * n))
```
- *d*: Distance in meters  
- *TxPower*: Signal strength at 1m  
- *RSSI*: Received Signal Strength Indicator  
- *n*: Path-loss exponent  

### A* Heuristic Function
```
f(n) = g(n) + h(n)
```
- *g(n)*: Cost from start node to current node  
- *h(n)*: Estimated cost from current node to goal  

---

## 📱 Features
✔️ Wi-Fi-based floor detection  
✔️ BLE trilateration for X–Y positioning  
✔️ A* algorithm for path optimization  
✔️ AR-based navigation overlay  
✔️ Real-time camera feed guidance  
✔️ Scalable to multi-floor environments  

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (>= Arctic Fox)  
- Unity 2021+ with ARCore SDK  
- BLE beacons configured (TxPower, UUID)  
- Wi-Fi APs mapped per floor  

### Clone Repository
```bash
git clone https://github.com/<your-username>/<repo-name>.git
```

### Run the Project
- Open `AndroidStudioProject/` in Android Studio  
- Configure dependencies (`gradle build`)  
- Deploy on a smartphone with ARCore support  

---

## 📊 Evaluation Metrics
- **Localization Accuracy** – Average positioning error in meters.  
- **Navigation Efficiency** – Path optimality compared to ground truth.  
- **User Experience** – Smoothness, latency, AR overlay clarity.  

---

## 📝 Research Contribution
This work demonstrates that **hybrid Wi-Fi + BLE localization** can achieve practical room-level accuracy (~1–2m) and AR-based navigation improves **user wayfinding indoors**.  
It provides insights for future **smart buildings**, **campus navigation apps**, and **AR-assisted guidance systems**.  

---

## 📂 Repository Structure
```
├── AndroidStudioProject/   # Mobile app source
├── UnityARProject/         # Unity AR navigation project
├── Docs/                   # Thesis, papers, diagrams
├── Scripts/                # Pathfinding, trilateration scripts
└── README.md               # Documentation
```

---

## 📖 References
- IEEE, ACM papers on Indoor Localization, Wi-Fi RSSI, BLE Trilateration, AR Navigation.  
*(Add your own citations list here)*  

---

## 📌 License
Distributed under the MIT License. See `LICENSE` for more information.

---

## 🤝 Acknowledgements
- **BCI Campus** – Academic support  
- **Research Supervisors** – Guidance & feedback  
- **Community & Open-Source Projects** – Inspiration and code snippets  

---
