package com.example.kivo.data.repositories

import com.example.kivo.data.models.WhoSaidThatQuestion

object WhoSaidThatRepository {
    val questions = listOf(
        // Reggaeton / Urbano
        WhoSaidThatQuestion(1, "Baby, la vida es un ciclo y lo que no sirve yo no lo reciclo", "Bad Bunny", listOf("Anuel AA", "J Balvin"), "Te Boté Remix", "Reggaetón", "Fácil", 2018),
        WhoSaidThatQuestion(2, "Me puse los Givenchy, me puse los Dior", "Rauw Alejandro", listOf("Feid", "Mora"), "Todo de Ti", "Pop Urbano", "Fácil", 2021),
        WhoSaidThatQuestion(3, "Tú eres mi bebita fiu fiu", "Tito Silva Music", listOf("Bad Bunny", "Karol G"), "Bebita Fiu Fiu", "Viral", "Fácil", 2022),
        WhoSaidThatQuestion(4, "Ella se entrega, no dice que no", "Alvaro Diaz", listOf("Rauw Alejandro", "Feid"), "Problemón", "Urbano", "Medio", 2021),
        WhoSaidThatQuestion(5, "Si te vas yo también me voy", "Paulo Londra", listOf("Duki", "Tiago PZK"), "Adán y Eva", "Trap", "Fácil", 2018),
        WhoSaidThatQuestion(6, "Provenza, nos fuimos pa' Medellín", "Karol G", listOf("Shakira", "Becky G"), "Provenza", "Reggaetón", "Fácil", 2022),
        WhoSaidThatQuestion(7, "Feliz cumpleaños, Ferxxo", "Feid", listOf("Mora", "Quevedo"), "Normal", "Reggaetón", "Fácil", 2022),
        WhoSaidThatQuestion(8, "Que la noche es larga y el mundo es pequeño", "Don Omar", listOf("Daddy Yankee", "Wisin"), "Dile", "Reggaetón", "Fácil", 2003),
        WhoSaidThatQuestion(9, "Lo que pasó, pasó entre los dos", "Daddy Yankee", listOf("Don Omar", "Nicky Jam"), "Lo Que Pasó, Pasó", "Reggaetón", "Fácil", 2004),
        WhoSaidThatQuestion(10, "Quédate, que la noche sin ti duele", "Quevedo", listOf("Bizarrap", "Duki"), "Bzrp Music Sessions, Vol. 52", "Urbano", "Fácil", 2022),
        
        // Pop / Rock Latino
        WhoSaidThatQuestion(11, "Clavado en un bar, hundido en el alcohol", "Maná", listOf("Enanitos Verdes", "Soda Stereo"), "Clavado en un Bar", "Rock Latino", "Fácil", 1997),
        WhoSaidThatQuestion(12, "De aquel amor de música ligera", "Soda Stereo", listOf("Caifanes", "Maná"), "De Música Ligera", "Rock Latino", "Fácil", 1990),
        WhoSaidThatQuestion(13, "Te felicito, qué bien actúas", "Shakira", listOf("Rosalía", "Tini"), "Te Felicito", "Pop", "Fácil", 2022),
        WhoSaidThatQuestion(14, "Eres mi religión, eres mi luz", "Maná", listOf("Reik", "Sin Bandera"), "Eres Mi Religión", "Pop Rock", "Fácil", 2002),
        WhoSaidThatQuestion(15, "Lamento boliviano, yo te amé", "Enanitos Verdes", listOf("Soda Stereo", "Vilma Palma"), "Lamento Boliviano", "Rock Latino", "Fácil", 1994),

        // Baladas / Clásicos
        WhoSaidThatQuestion(16, "Ya lo pasado, pasado, no me interesa", "José José", listOf("Luis Miguel", "Juan Gabriel"), "El Triste", "Balada", "Fácil", 1970),
        WhoSaidThatQuestion(17, "Querida, cada momento de mi vida siempre estás tú", "Juan Gabriel", listOf("Marco Antonio Solís", "Vicente Fernández"), "Querida", "Balada", "Fácil", 1984),
        WhoSaidThatQuestion(18, "Cuando calienta el sol aquí en la playa", "Luis Miguel", listOf("Chayanne", "Ricky Martin"), "Cuando Calienta el Sol", "Pop", "Fácil", 1987),
        WhoSaidThatQuestion(19, "Si no te hubieras ido sería tan feliz", "Marco Antonio Solís", listOf("Joan Sebastian", "Juan Gabriel"), "Si No Te Hubieras Ido", "Balada", "Fácil", 1999),
        WhoSaidThatQuestion(20, "Por mujeres como tú, amor", "Pepe Aguilar", listOf("Vicente Fernández", "Christian Nodal"), "Por Mujeres Como Tú", "Mariachi", "Fácil", 1998),

        // Internacional
        WhoSaidThatQuestion(21, "Just a small town girl, livin' in a lonely world", "Journey", listOf("Bon Jovi", "Queen"), "Don't Stop Believin'", "Rock", "Fácil", 1981),
        WhoSaidThatQuestion(22, "I'm a barbie girl, in a barbie world", "Aqua", listOf("Vengaboys", "Spice Girls"), "Barbie Girl", "Pop", "Fácil", 1997),
        WhoSaidThatQuestion(23, "Mama, just killed a man", "Queen", listOf("The Beatles", "Led Zeppelin"), "Bohemian Rhapsody", "Rock", "Fácil", 1975),
        WhoSaidThatQuestion(24, "Blinding lights, I'm drowning in the night", "The Weeknd", listOf("Bruno Mars", "Justin Bieber"), "Blinding Lights", "Synth-pop", "Fácil", 2019),
        WhoSaidThatQuestion(25, "I will always love you", "Whitney Houston", listOf("Celine Dion", "Mariah Carey"), "I Will Always Love You", "Balada", "Fácil", 1992),
        
        // Trap Argentino / Nuevo Urbano
        WhoSaidThatQuestion(26, "Y si nos vemos, nos vemos, no me digas nada", "Milo J", listOf("Duki", "Bizarrap"), "Milagrosa", "Urbano", "Medio", 2023),
        WhoSaidThatQuestion(27, "Goteo, goteo, toy' que goteo", "Duki", listOf("Khea", "Cazzu"), "Goteo", "Trap", "Fácil", 2019),
        WhoSaidThatQuestion(28, "Triple T, Tini, Tini, Tini", "Tini", listOf("Emilia", "Maria Becerra"), "La Loto", "Pop Urbano", "Fácil", 2022),
        WhoSaidThatQuestion(29, "Los del espacio, mami", "Lit Killah", listOf("Tiago PZK", "Rusherking"), "Los del Espacio", "Urbano", "Fácil", 2023),
        WhoSaidThatQuestion(30, "Miénteme, haz lo que quieras conmigo", "Tini", listOf("Maria Becerra", "Emilia"), "Miénteme", "Pop", "Fácil", 2021),
        
        // Más Urbano / Reggaeton
        WhoSaidThatQuestion(31, "Tengo una nota que no se me quita", "Jhayco", listOf("Feid", "Mora"), "Dákiti", "Reggaetón", "Fácil", 2020),
        WhoSaidThatQuestion(32, "Si tú supieras cuánto yo a ti te amo", "Anuel AA", listOf("Ozuna", "Bad Bunny"), "Sola Remix", "Trap", "Fácil", 2016),
        WhoSaidThatQuestion(33, "Criminal, cri-criminal", "Natti Natasha", listOf("Karol G", "Becky G"), "Criminal", "Reggaetón", "Fácil", 2017),
        WhoSaidThatQuestion(34, "Tú me partiste el corazón", "Maluma", listOf("J Balvin", "Nicky Jam"), "Corazón", "Reggaetón", "Fácil", 2017),
        WhoSaidThatQuestion(35, "Ella y yo, dos locos enamorados", "Aventura", listOf("Don Omar", "Romeo Santos"), "Ella y Yo", "Bachata", "Fácil", 2005),
        
        // Rock & Pop Internacional
        WhoSaidThatQuestion(36, "Another one bites the dust", "Queen", listOf("The Police", "AC/DC"), "Another One Bites the Dust", "Rock", "Fácil", 1980),
        WhoSaidThatQuestion(37, "Imagine all the people", "John Lennon", listOf("Paul McCartney", "Bob Dylan"), "Imagine", "Rock", "Fácil", 1971),
        WhoSaidThatQuestion(38, "Yellow submarine, yellow submarine", "The Beatles", listOf("The Rolling Stones", "Queen"), "Yellow Submarine", "Pop", "Fácil", 1966),
        WhoSaidThatQuestion(39, "Sweet child o' mine", "Guns N' Roses", listOf("Bon Jovi", "Aerosmith"), "Sweet Child O' Mine", "Hard Rock", "Fácil", 1987),
        WhoSaidThatQuestion(40, "Thriller, thriller night", "Michael Jackson", listOf("Prince", "Rick James"), "Thriller", "Pop", "Fácil", 1982),
        
        // Regional Mexicano / Corridos
        WhoSaidThatQuestion(41, "Ella baila sola, me dice que sí", "Peso Pluma", listOf("Natanael Cano", "Junior H"), "Ella Baila Sola", "Corridos", "Fácil", 2023),
        WhoSaidThatQuestion(42, "Adiós amor, me voy de ti", "Christian Nodal", listOf("Carin Leon", "Julión Álvarez"), "Adiós Amor", "Regional", "Fácil", 2017),
        WhoSaidThatQuestion(43, "Soy el ratón, soy el chapo", "Código FN", listOf("Peso Pluma", "Luis R Conriquez"), "Soy el Ratón", "Corridos", "Medio", 2021),
        WhoSaidThatQuestion(44, "Que se mueran los envidiosos", "Natanael Cano", listOf("Peso Pluma", "Junior H"), "AMG", "Corridos Tumbados", "Fácil", 2022),
        
        // Clásicos en Español
        WhoSaidThatQuestion(45, "Me gustas tú, me gusta el viento", "Manu Chao", listOf("Jarabe de Palo", "Café Tacvba"), "Me Gustas Tú", "Alternativo", "Fácil", 2001),
        WhoSaidThatQuestion(46, "Bonito, todo me parece bonito", "Jarabe de Palo", listOf("Bacilos", "Juanes"), "Bonito", "Pop Rock", "Fácil", 2003),
        WhoSaidThatQuestion(47, "A Dios le pido que si me muero sea de amor", "Juanes", listOf("Carlos Vives", "Juan Luis Guerra"), "A Dios le Pido", "Pop Rock", "Fácil", 2002),
        WhoSaidThatQuestion(48, "La flaca, por un beso de la flaca", "Jarabe de Palo", listOf("Maná", "Fito Páez"), "La Flaca", "Rock Latino", "Fácil", 1996),
        WhoSaidThatQuestion(49, "Rayando el sol, desesperación", "Maná", listOf("Enanitos Verdes", "Saga"), "Rayando el Sol", "Rock Latino", "Fácil", 1990),
        WhoSaidThatQuestion(50, "Ciega, sordomuda, traste, testaruda", "Shakira", listOf("Paulina Rubio", "Thalía"), "Ciega, Sordomuda", "Pop", "Fácil", 1998),
        
        // Más Internacional Pop
        WhoSaidThatQuestion(51, "I'm in love with the shape of you", "Ed Sheeran", listOf("Shawn Mendes", "Justin Bieber"), "Shape of You", "Pop", "Fácil", 2017),
        WhoSaidThatQuestion(52, "Hello from the other side", "Adele", listOf("Sia", "Lady Gaga"), "Hello", "Soul", "Fácil", 2015),
        WhoSaidThatQuestion(53, "Watermelon sugar high", "Harry Styles", listOf("Niall Horan", "Zayn"), "Watermelon Sugar", "Pop", "Fácil", 2019),
        WhoSaidThatQuestion(54, "I'm a survivor, I'm not gon' give up", "Destiny's Child", listOf("TLC", "Spice Girls"), "Survivor", "R&B", "Fácil", 2001),
        WhoSaidThatQuestion(55, "Umbrella, ella, ella, eh, eh", "Rihanna", listOf("Beyoncé", "Nicki Minaj"), "Umbrella", "R&B", "Fácil", 2007),
        
        // Trap & Rap
        WhoSaidThatQuestion(56, "God's plan, God's plan", "Drake", listOf("J. Cole", "Kanye West"), "God's Plan", "Rap", "Fácil", 2018),
        WhoSaidThatQuestion(57, "I'm the real Slim Shady", "Eminem", listOf("Dr. Dre", "Snoop Dogg"), "The Real Slim Shady", "Rap", "Fácil", 2000),
        WhoSaidThatQuestion(58, "Sicko mode, sun is down, freezin' cold", "Travis Scott", listOf("Drake", "Post Malone"), "Sicko Mode", "Trap", "Fácil", 2018),
        WhoSaidThatQuestion(59, "Humble, sit down, be humble", "Kendrick Lamar", listOf("Drake", "Big Sean"), "Humble", "Rap", "Fácil", 2017),
        WhoSaidThatQuestion(60, "Gin and juice, rollin' down the street", "Snoop Dogg", listOf("Dr. Dre", "Ice Cube"), "Gin and Juice", "Rap", "Fácil", 1993),
        
        // Bachata & Salsa
        WhoSaidThatQuestion(61, "Eres mi vida, eres mi cielo", "Marc Anthony", listOf("Victor Manuelle", "Gilberto Santa Rosa"), "Vivir Mi Vida", "Salsa", "Fácil", 2013),
        WhoSaidThatQuestion(62, "Propuesta indecente, a ver si te gusta", "Romeo Santos", listOf("Prince Royce", "Aventura"), "Propuesta Indecente", "Bachata", "Fácil", 2013),
        WhoSaidThatQuestion(63, "Darte un beso, que me suba el alma", "Prince Royce", listOf("Romeo Santos", "Juan Luis Guerra"), "Darte un Beso", "Bachata", "Fácil", 2013),
        WhoSaidThatQuestion(64, "La bilirrubina, me sube la bilirrubina", "Juan Luis Guerra", listOf("Carlos Vives", "Rubén Blades"), "La Bilirrubina", "Merengue", "Fácil", 1990),
        WhoSaidThatQuestion(65, "Tú me haces falta, mucha falta", "Luis Enrique", listOf("Marc Anthony", "Jerry Rivera"), "Yo No Sé Mañana", "Salsa", "Fácil", 2009),
        
        // Urbano 2024 (Futuro)
        WhoSaidThatQuestion(66, "Gata Only, ella es mi gata only", "FloyyMenor", listOf("Cris MJ", "Feid"), "Gata Only", "Urbano", "Fácil", 2024),
        WhoSaidThatQuestion(67, "Luna, dile que me perdone", "Feid", listOf("Mora", "Quevedo"), "Luna", "Reggaetón", "Fácil", 2023),
        WhoSaidThatQuestion(68, "Si antes te hubiera conocido", "Karol G", listOf("Rosalía", "Tini"), "Si Antes Te Hubiera Conocido", "Merengue", "Fácil", 2024),
        WhoSaidThatQuestion(69, "Perro Negro, ella es mi perro negro", "Bad Bunny", listOf("Feid", "Anuel AA"), "Perro Negro", "Reggaetón", "Fácil", 2023),
        WhoSaidThatQuestion(70, "Puntería, tú tienes puntería", "Shakira", listOf("Karol G", "Cardi B"), "Puntería", "Pop", "Fácil", 2024),
        
        // Más Rock Latino
        WhoSaidThatQuestion(71, "Cuando pase el temblor, despertaré", "Soda Stereo", listOf("Caifanes", "La Ley"), "Cuando Pase el Temblor", "Rock Latino", "Fácil", 1985),
        WhoSaidThatQuestion(72, "Afuera, afuera no te quiero ver", "Caifanes", listOf("Jaguares", "Maldita Vecindad"), "Afuera", "Rock Latino", "Fácil", 1994),
        WhoSaidThatQuestion(73, "Kumbala, bajo la luz de la luna", "Maldita Vecindad", listOf("Café Tacvba", "Panteón Rococó"), "Kumbala", "Rock Latino", "Fácil", 1991),
        WhoSaidThatQuestion(74, "Eres, lo que más quiero en este mundo", "Café Tacvba", listOf("Zoé", "Babasónicos"), "Eres", "Alternativo", "Fácil", 2003),
        WhoSaidThatQuestion(75, "Labios rotos, con tu mirada", "Zoé", listOf("Enjambre", "Hello Seahorse!"), "Labios Rotos", "Alternativo", "Fácil", 2011),
        
        // Más Pop 90s/2000s
        WhoSaidThatQuestion(76, "Provócame, mujer, provócame", "Chayanne", listOf("Ricky Martin", "Luis Miguel"), "Provócame", "Pop", "Fácil", 1992),
        WhoSaidThatQuestion(77, "Livin' la vida loca", "Ricky Martin", listOf("Enrique Iglesias", "Chayanne"), "Livin' la Vida Loca", "Pop", "Fácil", 1999),
        WhoSaidThatQuestion(78, "Bailamos, let the rhythm take you over", "Enrique Iglesias", listOf("Marc Anthony", "Ricky Martin"), "Bailamos", "Pop", "Fácil", 1999),
        WhoSaidThatQuestion(79, "Culpable o no, miénteme como siempre", "Luis Miguel", listOf("Cristian Castro", "Alejandro Fernández"), "Culpable o No", "Pop", "Fácil", 1988),
        WhoSaidThatQuestion(80, "No podrás, olvidar que te amé", "Cristian Castro", listOf("Luis Miguel", "Chayanne"), "No Podrás", "Pop", "Fácil", 1992),
        
        // Más Urbano Internacional
        WhoSaidThatQuestion(81, "I like it like that, you gotta believe me", "Cardi B", listOf("Nicki Minaj", "Megan Thee Stallion"), "I Like It", "Hip-Hop", "Fácil", 2018),
        WhoSaidThatQuestion(82, "Starboy, look what you've done", "The Weeknd", listOf("Daft Punk", "Drake"), "Starboy", "R&B", "Fácil", 2016),
        WhoSaidThatQuestion(83, "Despacito, quiero respirar tu cuello", "Luis Fonsi", listOf("Daddy Yankee", "Ricky Martin"), "Despacito", "Pop Urbano", "Fácil", 2017),
        WhoSaidThatQuestion(84, "Gasolina, dame más gasolina", "Daddy Yankee", listOf("Don Omar", "Tego Calderón"), "Gasolina", "Reggaetón", "Fácil", 2004),
        WhoSaidThatQuestion(85, "Danza Kuduro, la mano arriba", "Don Omar", listOf("Lucenzo", "Pitbull"), "Danza Kuduro", "Urbano", "Fácil", 2010),
        
        // Variados
        WhoSaidThatQuestion(86, "La vaca, la misma vaca", "Mala Fe", listOf("Elvis Crespo", "Oro Sólido"), "La Vaca", "Merengue", "Fácil", 1999),
        WhoSaidThatQuestion(87, "Suavemente, bésame", "Elvis Crespo", listOf("Juan Luis Guerra", "Sergio Vargas"), "Suavemente", "Merengue", "Fácil", 1998),
        WhoSaidThatQuestion(88, "Obsesión, son las cinco de la mañana", "Aventura", listOf("Romeo Santos", "Prince Royce"), "Obsesión", "Bachata", "Fácil", 2002),
        WhoSaidThatQuestion(89, "La camisa negra, hoy tengo el alma negra", "Juanes", listOf("Carlos Vives", "Shakira"), "La Camisa Negra", "Pop Rock", "Fácil", 2004),
        WhoSaidThatQuestion(90, "La gota fría, que me da la gana", "Carlos Vives", listOf("Juanes", "Fonseca"), "La Gota Fría", "Vallenato", "Fácil", 1993),
        
        // Más actual
        WhoSaidThatQuestion(91, "Hielos, mami, yo no quiero hielos", "Mora", listOf("Feid", "Jhayco"), "Hielos", "Reggaetón", "Fácil", 2020),
        WhoSaidThatQuestion(92, "Piel de seda, labios de fresa", "Alvaro Diaz", listOf("Rauw Alejandro", "Feid"), "LENTITO", "Urbano", "Fácil", 2022),
        WhoSaidThatQuestion(93, "Bizarrap, biza, biza", "Bizarrap", listOf("Quevedo", "Duki"), "Bzrp Music Sessions", "Urbano", "Fácil", 2022),
        WhoSaidThatQuestion(94, "TQG, te quedé grande", "Karol G & Shakira", listOf("Tini", "Becky G"), "TQG", "Reggaetón", "Fácil", 2023),
        WhoSaidThatQuestion(95, "Yandel 150", "Yandel & Feid", listOf("Daddy Yankee", "Don Omar"), "Yandel 150", "Reggaetón", "Fácil", 2023),
        
        // Clásicos Rock
        WhoSaidThatQuestion(96, "Smells like teen spirit", "Nirvana", listOf("Pearl Jam", "Soundgarden"), "Smells Like Teen Spirit", "Grunge", "Fácil", 1991),
        WhoSaidThatQuestion(97, "With or without you", "U2", listOf("R.E.M.", "The Cure"), "With or Without You", "Rock", "Fácil", 1987),
        WhoSaidThatQuestion(98, "Wonderwall, today is gonna be the day", "Oasis", listOf("Blur", "The Verve"), "Wonderwall", "Britpop", "Fácil", 1995),
        WhoSaidThatQuestion(99, "Eye of the tiger, it's the thrill of the fight", "Survivor", listOf("Journey", "Europe"), "Eye of the Tiger", "Rock", "Fácil", 1982),
        WhoSaidThatQuestion(100, "Livin' on a prayer", "Bon Jovi", listOf("Guns N' Roses", "Van Halen"), "Livin' on a Prayer", "Rock", "Fácil", 1986),
        
        // Especial Bad Bunny (Nuevas canciones)
        WhoSaidThatQuestion(101, "Siento que el tiempo se acaba, que ya no somos lo mismo", "Bad Bunny", listOf("Rauw Alejandro", "Anuel AA"), "haciendo que me amas", "Trap", "Medio", 2020),
        WhoSaidThatQuestion(102, "Yo soy un rey, campeón, Booker T", "Bad Bunny", listOf("Duki", "Myke Towers"), "BOOKER T", "Trap", "Fácil", 2020),
        WhoSaidThatQuestion(103, "Yo no me quiero casar, a menos que sea contigo", "Bad Bunny", listOf("Feid", "Mora"), "NO ME QUIERO CASAR", "Urbano", "Fácil", 2023),
        WhoSaidThatQuestion(104, "Vete, nadie te está aguantando", "Bad Bunny", listOf("Karol G", "Rosalía"), "Vete", "Reggaetón", "Fácil", 2019),
        WhoSaidThatQuestion(105, "Tú no metes cabra, saramambiche", "Bad Bunny", listOf("Cosculluela", "Anuel AA"), "Tu No Metes Cabra", "Trap", "Fácil", 2017),
        WhoSaidThatQuestion(106, "Baby, te deseo lo mejor", "Bad Bunny", listOf("J Balvin", "Maluma"), "TE DESEO LO MEJOR", "Pop Rock", "Fácil", 2020),
        WhoSaidThatQuestion(107, "A 120 por la autopista", "Bad Bunny", listOf("Mora", "Quevedo"), "120", "Urbano", "Medio", 2020),
        WhoSaidThatQuestion(108, "Baby, dime si una vez nos podemos ver", "Mora & Bad Bunny", listOf("Feid", "Jhayco"), "Una Vez", "Reggaetón", "Fácil", 2020),
        WhoSaidThatQuestion(109, "Donde ella va, yo voy", "Bad Bunny", listOf("Rauw Alejandro", "The Weeknd"), "WHERE SHE GOES", "Jersey Club", "Fácil", 2023),
        WhoSaidThatQuestion(110, "Si estuviésemos juntos, nada nos faltaría", "Bad Bunny", listOf("Ozuna", "Shakira"), "Si Estuviésemos Juntos", "Urbano", "Fácil", 2018),
        WhoSaidThatQuestion(111, "No me busques más, que ya me mudaste", "Bad Bunny", listOf("Tini", "Emilia"), "TE MUDASTE", "Pop Urbano", "Fácil", 2020),
        WhoSaidThatQuestion(112, "Tú me encantas más que el chocolate", "Tony Dize & Bad Bunny", listOf("Don Omar", "Zion"), "La Corriente", "Reggaetón", "Fácil", 2022),
        WhoSaidThatQuestion(113, "Te gusto porque soy un diablo", "Natanael Cano & Bad Bunny", listOf("Junior H", "Peso Pluma"), "Soy El Diablo Remix", "Corridos Tumbados", "Fácil", 2019),
        WhoSaidThatQuestion(114, "Antes yo te quería, ahora no", "Bad Bunny", listOf("Arcángel", "De La Ghetto"), "Soy Peor", "Trap", "Fácil", 2016),
        WhoSaidThatQuestion(115, "A mí me gustan mayores, de esos que llaman señores", "Becky G & Bad Bunny", listOf("Karol G", "Natti Natasha"), "Mayores", "Pop Urbano", "Fácil", 2017),
        WhoSaidThatQuestion(116, "No me vuelvas a decir bebé", "Bad Bunny", listOf("Rosalía", "Tini"), "Solo de Mi", "Urbano", "Fácil", 2018),
        WhoSaidThatQuestion(117, "Siete de la mañana en Miami", "Bad Bunny", listOf("Mora", "Feid"), "Otra Noche en Miami", "Synth-pop", "Fácil", 2018),
        WhoSaidThatQuestion(118, "Chambea, jala, cabrón, ya no te quedan balas", "Bad Bunny", listOf("Anuel AA", "Ñengo Flow"), "Chambea", "Trap", "Fácil", 2017),
        WhoSaidThatQuestion(119, "Perro negro, ella es mi perro negro", "Feid & Bad Bunny", listOf("Mora", "Quevedo"), "PERRO NEGRO", "Reggaetón", "Fácil", 2023),
        WhoSaidThatQuestion(120, "No me ames, te lo ruego", "Bad Bunny", listOf("Marc Anthony", "Luis Miguel"), "Amorfoda", "Balada", "Fácil", 2018)
    )
    
    // Function to get random questions by category and difficulty
    fun getQuestions(count: Int, genre: String? = null, difficulty: String? = null): List<WhoSaidThatQuestion> {
        var filtered = questions
        if (genre != null && genre != "Todos") {
            filtered = filtered.filter { it.genre.contains(genre, ignoreCase = true) }
        }
        if (difficulty != null && difficulty != "Cualquiera") {
            filtered = filtered.filter { it.difficulty.equals(difficulty, ignoreCase = true) }
        }
        return filtered.shuffled().take(count)
    }
}
