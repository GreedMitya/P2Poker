# P2Poker 🎲

**P2Poker** Peer-to-peer poker game over local network, built with LibGDX and KryoNet. 
No central server required.

---


## 💡 Why I built this
This project was built as part of my learning journey in game development and network programming.  
I wanted to create a real multiplayer system with a working UI and full poker logic from scratch.

---


## 📦 Features:
- LAN poker for 2–6 players  
- Client and server bundled in the same project  
- LibGDX + Scene2D for graphics and UI  
- Displays cards, bets, balances, and winners  
- Deal and pot animations  
- Modular screen architecture: `LobbyScreen`, `GameScreen`

---

## 📈 Project Status

✅ Fully functional  
🧪 Tested with 2–6 players  
🚧 Currently in LAN-only mode (no internet support planned yet)

---

## 📸 Screenshots:

<img width="811" height="477" alt="{54FCD879-7969-4D63-85E0-687B5BEE7894}" src="https://github.com/user-attachments/assets/6bddc589-0763-471f-b8cb-cba48797899b" />

<img width="810" height="469" alt="{4DB0C3BC-286E-4C39-9561-19D16D7BC0E0}" src="https://github.com/user-attachments/assets/8ef79744-323d-4d19-bb6a-2b585b97cdfb" />

<img width="802" height="469" alt="{B62BCA47-1ACF-4164-A625-03C28C5EB118}" src="https://github.com/user-attachments/assets/f6b22a65-8b05-40fb-97d3-55607423940b" />

<img width="800" height="469" alt="{974569E0-6FFD-41D0-BFAA-1FCD9A5BA23E}" src="https://github.com/user-attachments/assets/cb915e2a-51ff-4053-a9d3-f5856f31baca" />

<img width="802" height="468" alt="{8C90E6CB-39C1-4244-900C-71BF76E00B52}" src="https://github.com/user-attachments/assets/3d1f474c-e5f2-45b2-a10a-2ef3b25b2b58" />

## 🚀 Getting Started

### 🧪 Option 1: Run from Source

1. Make sure you have **JDK 17+** installed
2. Build and run with Gradle:
   ```bash
   ./gradlew :lwjgl3:run
   ```

### 💻 Option 2: Download Executable (Windows)

You can download the ready-to-use `.exe` file here:


🔗 Or browse the full release page: [Releases v1.0.5](https://github.com/GreedMitya/P2Poker/releases/tag/v1.0.5)

---

## 🧪 Tech Stack:
- Java 17
- LibGDX
- KryoNet
- Scene2D
- Gradle

---

## 🎯 Fairness and Evaluation
- Test Code: HandEvaluatorTest.java
- Simulates 1,000,000 random hands between two players
- Just run it and compare hand distributions to real probability
(Also includes validation for correct hand evaluation — try // testKnownHands();)
<img width="606" height="600" alt="Probability Test" src="https://github.com/user-attachments/assets/0f124052-6064-4da1-afea-ed616504ef3c" />

--- 

## 🔧 Contributing
Feel free to fork this repo and submit pull requests. All ideas are welcome.


---


## 🧾 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---


## 👤 Author

[GreedMitya](https://github.com/GreedMitya)  
Email: senseimitya@gmail.com

---


## 🏷️ Tags
Java, Poker, GameDev, LibGDX, Multiplayer, Networking, Pet Project

---



## P2Poker 🎲
P2Poker — LAN покер-клиент, написанный на Java с использованием LibGDX и KryoNet.
Позволяет играть с другими игроками в локальной сети без централизованного сервера.

## 📦 Особенности:
- LAN покер на 2–6 игроков

- Клиент и сервер внутри одного проекта

- LibGDX + Scene2D для графики и UI

- Отображение карт, ставок, баланса и выигрыша

- Анимации раздачи и сбора банка

- Архитектура через экраны: LobbyScreen, GameScreen

## 🚀 Запуск:
Убедитесь, что установлен JDK 17+

Соберите через Gradle:

bash
Копировать
Редактировать
./gradlew :lwjgl3:run
Альтернатива — скачайте .exe в Releases

## 🧪 Технологии:
- Java 17
- LibGDX
- KryoNet
- Scene2D
- Gradle

## Проверка честности и баланса

 - TestCode: [`HandEvaluatorTest.java`](HandEvaluatorTest.java)
 - Симуляция 1,000,000 случайных раздач между двумя руками:
 - Просто запустить и сверить выборку рук и комбинаций с теорией вероятности!
(Так же, внутри, есть функция проверки правильности определения комбинаций! "// testKnownHands();" )


