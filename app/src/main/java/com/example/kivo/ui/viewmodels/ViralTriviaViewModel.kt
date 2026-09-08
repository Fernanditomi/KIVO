package com.example.kivo.ui.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

enum class QuestionDifficulty {
    EASY, NORMAL, HARD
}

data class Question(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctAnswer: String,
    val difficulty: QuestionDifficulty
)

data class ViralTriviaState(
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isGameOver: Boolean = false,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val questions: List<Question> = emptyList(),
    val selectedDifficulty: QuestionDifficulty? = null
)

class ViralTriviaViewModel : ViewModel() {
    private val allQuestions = listOf(
        // FÁCIL
        Question(1, "¿Qué artista femenina ostenta el récord histórico de más premios Grammy ganados?", listOf("Beyoncé", "Taylor Swift", "Adele"), "Beyoncé", QuestionDifficulty.EASY),
        Question(3, "¿Qué grupo de K-Pop integran RM, Jin, Suga, J-Hope, Jimin, V y Jungkook?", listOf("BTS", "EXO", "BLACKPINK"), "BTS", QuestionDifficulty.EASY),
        Question(5, "¿Qué famoso festival de música se celebra anualmente en el desierto de Indio, California?", listOf("Coachella", "Tomorrowland", "Lollapalooza"), "Coachella", QuestionDifficulty.EASY),
        Question(10, "¿Qué cantante es apodada mundialmente como la 'Reina del Pop'?", listOf("Madonna", "Lady Gaga", "Britney Spears"), "Madonna", QuestionDifficulty.EASY),
        Question(11, "¿Quién es conocido mundialmente como el 'Rey del Rock and Roll'?", listOf("Elvis Presley", "Chuck Berry", "Little Richard"), "Elvis Presley", QuestionDifficulty.EASY),
        Question(12, "¿Qué banda británica es la autora del éxito 'Bohemian Rhapsody'?", listOf("Queen", "The Beatles", "Led Zeppelin"), "Queen", QuestionDifficulty.EASY),
        Question(13, "¿Qué artista lanzó el álbum 'Thriller', el más vendido de la historia?", listOf("Michael Jackson", "Prince", "George Michael"), "Michael Jackson", QuestionDifficulty.EASY),
        Question(16, "¿Cómo se le llama oficialmente al club de fans de Taylor Swift?", listOf("Swifties", "Beliebers", "Arianators"), "Swifties", QuestionDifficulty.EASY),
        Question(40, "¿Qué artista tiene el récord de la gira musical más recaudadora de la historia (The Eras Tour)?", listOf("Taylor Swift", "Beyoncé", "Madonna"), "Taylor Swift", QuestionDifficulty.EASY),
        Question(25, "¿Cómo se le conoce mundialmente al legendario cantante José José?", listOf("El Príncipe de la Canción", "El Sol de México", "El Rey"), "El Príncipe de la Canción", QuestionDifficulty.EASY),

        // NORMAL
        Question(2, "¿En qué ciudad y barrio nació la cultura y música Hip-Hop durante los años 70?", listOf("El Bronx, Nueva York", "Brooklyn, Nueva York", "Compton, California"), "El Bronx, Nueva York", QuestionDifficulty.NORMAL),
        Question(4, "¿Cuál fue el primer álbum completamente en español nominado a 'Álbum del Año' en los Grammy?", listOf("Un Verano Sin Ti - Bad Bunny", "Saturno - Rauw Alejandro", "Motomami - Rosalía"), "Un Verano Sin Ti - Bad Bunny", QuestionDifficulty.NORMAL),
        Question(6, "¿En qué país caribeño se originó el ritmo del Reggae?", listOf("Jamaica", "Puerto Rico", "Cuba"), "Jamaica", QuestionDifficulty.NORMAL),
        Question(7, "¿Qué género dominicano se toca tradicionalmente con guitarra, requinto, güira y bongó?", listOf("Bachata", "Salsa", "Merengue"), "Bachata", QuestionDifficulty.NORMAL),
        Question(15, "¿En qué deporte profesional intentó hacer carrera Rauw antes de ser músico?", listOf("Fútbol", "Baloncesto", "Béisbol"), "Fútbol", QuestionDifficulty.NORMAL),
        Question(18, "¿En qué país nació Rauw Alejandro?", listOf("Puerto Rico (San Juan)", "República Dominicana", "Cuba"), "Puerto Rico (San Juan)", QuestionDifficulty.NORMAL),
        Question(19, "¿Cómo se titula el esperado álbum lanzado por Alvaro Diaz en 2024?", listOf("Sayonara", "Felicilandia", "Diaz Antes"), "Sayonara", QuestionDifficulty.NORMAL),
        Question(20, "¿Con qué artista colabora Alvaro Diaz en el éxito 'Problemón'?", listOf("Rauw Alejandro", "Feid", "Bad Bunny"), "Rauw Alejandro", QuestionDifficulty.NORMAL),
        Question(22, "¿Qué canción de Latin Mafia se volvió un fenómeno viral en TikTok?", listOf("Julietota", "Patadas de Ahogado", "Salida"), "Julietota", QuestionDifficulty.NORMAL),
        Question(24, "¿De qué país es originario el grupo Latin Mafia?", listOf("México", "Colombia", "Puerto Rico"), "México", QuestionDifficulty.NORMAL),
        Question(28, "¿De qué ciudad argentina es originario Paulo Londra?", listOf("Córdoba", "Buenos Aires", "Rosario"), "Córdoba", QuestionDifficulty.NORMAL),
        Question(29, "¿Cuál fue el primer gran éxito que lanzó a la fama a Paulo Londra?", listOf("Relax", "Adán y Eva", "Nena Maldición"), "Relax", QuestionDifficulty.NORMAL),
        Question(31, "¿Cómo se titula el primer álbum de estudio de Milo J lanzado en 2023?", listOf("111", "En dormir sin Madrid", "511"), "111", QuestionDifficulty.NORMAL),
        Question(39, "¿Qué género musical nació en las comunidades negras y latinas del sur del Bronx?", listOf("Hip-Hop", "Punk", "Disco"), "Hip-Hop", QuestionDifficulty.NORMAL),

        // DIFÍCIL
        Question(8, "¿En qué ciudad estadounidense nació el Jazz a finales del siglo XIX?", listOf("Nueva Orleans", "Chicago", "Detroit"), "Nueva Orleans", QuestionDifficulty.HARD),
        Question(9, "¿Qué subgénero electrónico se caracteriza por ritmos rápidos y bajos potentes?", listOf("EDM / Techno", "Reggaeton", "Trap"), "EDM / Techno", QuestionDifficulty.HARD),
        Question(14, "¿Cuál es el nombre de nacimiento de Rauw Alejandro?", listOf("Raúl Alejandro Ocasio Ruiz", "Raúl Alejandro Ruiz Ocasio", "Juan Alejandro Ocasio"), "Raúl Alejandro Ocasio Ruiz", QuestionDifficulty.HARD),
        Question(17, "¿Cuál es la temática principal del álbum Saturno (2022)?", listOf("Ciencia ficción y alienígenas", "El desierto", "Mundo medieval"), "Ciencia ficción y alienígenas", QuestionDifficulty.HARD),
        Question(21, "¿Cuál es el apodo o marca visual frecuente de Alvaro Diaz?", listOf("Llandel", "Sad Boy", "El de la Suerte"), "Sad Boy", QuestionDifficulty.HARD),
        Question(23, "¿Cómo está integrado el grupo Latin Mafia?", listOf("Tres hermanos", "Dos primos", "Tres amigos de escuela"), "Tres hermanos", QuestionDifficulty.HARD),
        Question(26, "¿Cuál de estas es una de las interpretaciones más icónicas de José José en el festival OTI?", listOf("El Triste", "La Nave del Olvido", "Almohada"), "El Triste", QuestionDifficulty.HARD),
        Question(27, "¿Qué instrumento tocaba José José además de cantar?", listOf("Contrabajo", "Trompeta", "Batería"), "Contrabajo", QuestionDifficulty.HARD),
        Question(30, "¿Qué productor colaboró con Paulo en su regreso musical tras su conflicto legal?", listOf("Ovy on the Drums", "Bizarrap", "Tainy"), "Bizarrap", QuestionDifficulty.HARD),
        Question(32, "¿Qué edad tenía Milo J cuando grabó su famosa BZRP Music Session?", listOf("16 años", "18 años", "20 años"), "16 años", QuestionDifficulty.HARD),
        Question(33, "¿Cuál es el barrio de Buenos Aires del que Milo J suele hacer referencia?", listOf("Morón", "La Boca", "Palermo"), "Morón", QuestionDifficulty.HARD),
        Question(34, "¿Qué instrumento se conoce como 'el rey de los instrumentos'?", listOf("Órgano", "Piano", "Violín"), "Órgano", QuestionDifficulty.HARD),
        Question(35, "¿En qué década surgió el movimiento Grunge en Seattle?", listOf("Años 90", "Años 80", "Años 70"), "Años 90", QuestionDifficulty.HARD),
        Question(36, "¿Qué subgénero del metal se caracteriza por voces guturales y ritmos extremadamente rápidos?", listOf("Death Metal", "Heavy Metal", "Power Metal"), "Death Metal", QuestionDifficulty.HARD),
        Question(37, "¿Quién es la compositora del famoso tema 'Bésame Mucho'?", listOf("Consuelo Velázquez", "Chavela Vargas", "Natalia Lafourcade"), "Consuelo Velázquez", QuestionDifficulty.HARD),
        Question(38, "¿Cómo se llama el vocalista y líder de la banda The Rolling Stones?", listOf("Mick Jagger", "Keith Richards", "Freddie Mercury"), "Mick Jagger", QuestionDifficulty.HARD),
        // Nuevas preguntas de Bad Bunny y otros
        Question(41, "¿A qué álbum pertenece la canción 'Si Estuviésemos Juntos' de Bad Bunny?", listOf("X 100PRE", "YHLQMDLG", "Oasis"), "X 100PRE", QuestionDifficulty.NORMAL),
        Question(42, "¿Qué luchador de la WWE inspiró una canción del álbum El Último Tour del Mundo?", listOf("Booker T", "The Rock", "John Cena"), "Booker T", QuestionDifficulty.EASY),
        Question(43, "¿En qué video musical Bad Bunny se viste de mujer para transmitir un mensaje contra la violencia de género?", listOf("Yo Perreo Sola", "Solo de Mi", "Caro"), "Yo Perreo Sola", QuestionDifficulty.EASY),
        Question(44, "¿Con qué artista colabora Bad Bunny en la canción 'Una Vez'?", listOf("Mora", "Jhayco", "Tainy"), "Mora", QuestionDifficulty.NORMAL),
        Question(45, "¿Cuál es el género principal de la canción 'Después de la Playa'?", listOf("Mambo / Merengue", "Reggaetón", "Bachata"), "Mambo / Merengue", QuestionDifficulty.NORMAL),
        Question(46, "¿Qué canción de Bad Bunny y Feid se volvió un éxito instantáneo en el álbum 'nadie sabe lo que va a pasar mañana'?", listOf("PERRO NEGRO", "MONACO", "FINA"), "PERRO NEGRO", QuestionDifficulty.EASY),
        Question(47, "¿A qué álbum pertenece la canción '120'?", listOf("El Último Tour del Mundo", "Un Verano Sin Ti", "YHLQMDLG"), "El Último Tour del Mundo", QuestionDifficulty.HARD),
        Question(48, "¿Cuál fue el primer álbum de estudio de Bad Bunny lanzado en 2018?", listOf("X 100PRE", "Oasis", "Afrodisíaco"), "X 100PRE", QuestionDifficulty.NORMAL),
        Question(49, "¿Qué canción de Bad Bunny es una balada acústica de despecho lanzada en San Valentín?", listOf("Amorfoda", "Solo de Mi", "Si Estuviésemos Juntos"), "Amorfoda", QuestionDifficulty.EASY),
        Question(50, "¿En qué canción colabora Bad Bunny con Becky G?", listOf("Mayores", "Sin Pijama", "Ram Pam Pam"), "Mayores", QuestionDifficulty.EASY)
    )

    private val _state = mutableStateOf(ViralTriviaState())
    val state: State<ViralTriviaState> = _state

    fun startNewGame(difficulty: QuestionDifficulty) {
        val filteredQuestions = allQuestions.filter { it.difficulty == difficulty }.shuffled().take(10)
        _state.value = ViralTriviaState(questions = filteredQuestions, selectedDifficulty = difficulty)
    }

    fun submitAnswer(answer: String) {
        if (_state.value.selectedAnswer != null) return

        val currentQuestion = _state.value.questions[_state.value.currentQuestionIndex]
        val isCorrect = answer == currentQuestion.correctAnswer
        
        val multiplier = when (_state.value.selectedDifficulty) {
            QuestionDifficulty.EASY -> 1
            QuestionDifficulty.NORMAL -> 2
            QuestionDifficulty.HARD -> 3
            else -> 1
        }
        
        val points = if (isCorrect) 100 * multiplier else 0
        val newScore = _state.value.score + points

        _state.value = _state.value.copy(
            selectedAnswer = answer,
            isCorrect = isCorrect,
            score = newScore
        )
    }

    fun nextQuestion() {
        if (_state.value.currentQuestionIndex < _state.value.questions.size - 1) {
            _state.value = _state.value.copy(
                currentQuestionIndex = _state.value.currentQuestionIndex + 1,
                selectedAnswer = null,
                isCorrect = null
            )
        } else {
            _state.value = _state.value.copy(isGameOver = true)
        }
    }

    fun resetToMenu() {
        _state.value = ViralTriviaState()
    }
}
