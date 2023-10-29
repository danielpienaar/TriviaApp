# TriviaApp
Trivia game for android written in Kotlin. Allows users to choose an online or offline game.
Fetches data from [Open Trivia Database API](https://opentdb.com/api_config.php) using the retrofit v2 library. Online mode directly uses this data, and offline mode caches this data in a local SQLite database, which can be refreshed whenever.
