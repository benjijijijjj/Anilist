package anilist

/*
 * AniList - Bosquejo (sketch) en Kotlin puro / JVM
 * Solo stdlib de Kotlin: sin Android SDK ni frameworks externos.
 *
 * Decisiones de diseño:
 *  - TituloAudiovisual es abstracta; Anime y Manga son sus subclases concretas.
 *  - Seguimiento es la clase asociativa Usuario <-> TituloAudiovisual y
 *    es la única que guarda estado, progreso, puntuación y favorito.
 *  - Las colecciones mutables son privadas y se exponen como solo lectura.
 *  - Genero y Tipo no conocen a los títulos (relación unidireccional) para
 *    evitar ciclos en equals/hashCode/toString de las data classes.
 */

// ---------------------------------------------------------------------------
// Enumeración de estados
// ---------------------------------------------------------------------------

/** Estado en que un [Usuario] tiene un [TituloAudiovisual] dentro de su lista. */
enum class EstadoSeguimiento(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    VIENDO("Viendo / Leyendo"),
    COMPLETADO("Completado"),
    ABANDONADO("Abandonado")
}

// ---------------------------------------------------------------------------
// Clases de categorización
// ---------------------------------------------------------------------------

/** Formato del título (ej: TV, Película, OVA, Manga). Relación N —— 1 con el título. */
data class Tipo(val nombre: String)

/** Categoría temática del título (ej: Acción, Romance, Isekai). Relación N —— N con el título. */
data class Genero(val nombre: String)

// ---------------------------------------------------------------------------
// Título audiovisual (abstracta) y subclases
// ---------------------------------------------------------------------------

/**
 * Clase base de cualquier producto audiovisual del catálogo.
 *
 * @property id identificador único dentro del catálogo.
 * @property nombre nombre principal del título.
 * @property sinopsis resumen opcional.
 * @property tipo formato del título (N —— 1).
 */
abstract class TituloAudiovisual(
    val id: Int,
    val nombre: String,
    val sinopsis: String?,
    val tipo: Tipo,
    generosIniciales: Set<Genero> = emptySet()
) {
    private val _generos: MutableSet<Genero> = generosIniciales.toMutableSet()
    private val _seguimientos: MutableList<Seguimiento> = mutableListOf()

    /** Géneros del título (N —— N), expuestos como solo lectura. */
    val generos: Set<Genero> get() = _generos

    /** Seguimientos de los usuarios sobre este título (1 —— N). */
    val seguimientos: List<Seguimiento> get() = _seguimientos

    /** Unidad en la que se mide el progreso (ej: "episodios", "capítulos"). */
    abstract val unidadProgreso: String

    /** Total de unidades si se conoce; `null` si está en emisión o es desconocido. */
    abstract val totalUnidades: Int?

    /** Descripción corta para mostrar en tarjetas del catálogo. */
    abstract fun descripcionCorta(): String

    /** Puntuación promedio entre todos los usuarios, o `null` si nadie ha puntuado. */
    val puntuacionPromedio: Double?
        get() = _seguimientos.mapNotNull { it.puntuacion }
            .takeIf { it.isNotEmpty() }
            ?.average()

    /** Asocia un [genero] al título. */
    fun agregarGenero(genero: Genero): Boolean = _generos.add(genero)

    /** Registra un seguimiento; lo invoca [Usuario.seguir] para mantener ambos lados sincronizados. */
    internal fun registrarSeguimiento(seguimiento: Seguimiento) {
        _seguimientos.add(seguimiento)
    }

    override fun toString(): String = "${this::class.simpleName}(id=$id, nombre='$nombre')"
}

/**
 * Anime: título audiovisual episódico.
 *
 * @property episodios cantidad de episodios (`null` si sigue en emisión).
 * @property estudio estudio de animación.
 */
class Anime(
    id: Int,
    nombre: String,
    tipo: Tipo,
    generos: Set<Genero> = emptySet(),
    sinopsis: String? = null,
    val episodios: Int? = null,
    val estudio: String? = null
) : TituloAudiovisual(id, nombre, sinopsis, tipo, generos) {

    override val unidadProgreso: String = "episodios"
    override val totalUnidades: Int? get() = episodios

    override fun descripcionCorta(): String =
        "$nombre [${tipo.nombre}] - ${episodios ?: "?"} episodios" +
            (estudio?.let { " - Estudio: $it" } ?: "")
}

/**
 * Manga: título de lectura por capítulos.
 *
 * @property capitulos cantidad de capítulos (`null` si sigue publicándose).
 * @property autor autor o autora.
 */
class Manga(
    id: Int,
    nombre: String,
    tipo: Tipo,
    generos: Set<Genero> = emptySet(),
    sinopsis: String? = null,
    val capitulos: Int? = null,
    val autor: String? = null
) : TituloAudiovisual(id, nombre, sinopsis, tipo, generos) {

    override val unidadProgreso: String = "capítulos"
    override val totalUnidades: Int? get() = capitulos

    override fun descripcionCorta(): String =
        "$nombre [${tipo.nombre}] - ${capitulos ?: "?"} capítulos" +
            (autor?.let { " - Autor: $it" } ?: "")
}

// ---------------------------------------------------------------------------
// Clase asociativa
// ---------------------------------------------------------------------------

/**
 * Clase asociativa entre [Usuario] y [TituloAudiovisual].
 * Guarda los datos propios de la relación: estado, progreso, puntuación y favorito.
 *
 * Se crea mediante [Usuario.seguir] para mantener las colecciones de ambos lados sincronizadas.
 */
data class Seguimiento internal constructor(
    val usuario: Usuario,
    val titulo: TituloAudiovisual,
    var estado: EstadoSeguimiento = EstadoSeguimiento.PENDIENTE,
    var progreso: Int = 0,
    var puntuacion: Int? = null,
    var favorito: Boolean = false
) {
    /** Avanza el progreso; marca como completado si se alcanza el total conocido. */
    fun avanzar(unidades: Int = 1) {
        require(unidades > 0) { "Las unidades a avanzar deben ser positivas" }
        if (estado == EstadoSeguimiento.PENDIENTE) estado = EstadoSeguimiento.VIENDO
        progreso += unidades
        titulo.totalUnidades?.let { total ->
            if (progreso >= total) {
                progreso = total
                estado = EstadoSeguimiento.COMPLETADO
            }
        }
    }

    /** Asigna una puntuación de 1 a 10. */
    fun puntuar(valor: Int) {
        require(valor in 1..10) { "La puntuación debe estar entre 1 y 10" }
        puntuacion = valor
    }

    /** Alterna la marca de favorito. */
    fun alternarFavorito() {
        favorito = !favorito
    }

    /** Abandona el título conservando el progreso actual. */
    fun abandonar() {
        estado = EstadoSeguimiento.ABANDONADO
    }

    // Se sobrescribe para evitar recursión infinita Usuario <-> Seguimiento <-> Título.
    override fun toString(): String =
        "Seguimiento(${usuario.nombre} -> ${titulo.nombre}, estado=$estado, " +
            "progreso=$progreso/${titulo.totalUnidades ?: "?"} ${titulo.unidadProgreso}, " +
            "puntuacion=${puntuacion ?: "-"}, favorito=$favorito)"
}

// ---------------------------------------------------------------------------
// Usuario
// ---------------------------------------------------------------------------

/**
 * Usuario del sistema (uso libre, sin autenticación en esta fase).
 * Gestiona sus catálogos a través de sus [seguimientos].
 */
class Usuario(val id: Int, val nombre: String) {
    private val _seguimientos: MutableList<Seguimiento> = mutableListOf()

    /** Seguimientos del usuario (1 —— N), solo lectura. */
    val seguimientos: List<Seguimiento> get() = _seguimientos

    /** Títulos marcados como favoritos. */
    val favoritos: List<TituloAudiovisual>
        get() = _seguimientos.filter { it.favorito }.map { it.titulo }

    /** Empieza a seguir un [titulo]; falla si ya existe un seguimiento para él. */
    fun seguir(
        titulo: TituloAudiovisual,
        estado: EstadoSeguimiento = EstadoSeguimiento.PENDIENTE
    ): Seguimiento {
        require(buscarSeguimiento(titulo) == null) { "Ya sigues '${titulo.nombre}'" }
        val seguimiento = Seguimiento(usuario = this, titulo = titulo, estado = estado)
        _seguimientos.add(seguimiento)
        titulo.registrarSeguimiento(seguimiento)
        return seguimiento
    }

    /** Busca el seguimiento de este usuario para un [titulo], si existe. */
    fun buscarSeguimiento(titulo: TituloAudiovisual): Seguimiento? =
        _seguimientos.firstOrNull { it.titulo == titulo }

    /** Lista los seguimientos con un [estado] dado. */
    fun porEstado(estado: EstadoSeguimiento): List<Seguimiento> =
        _seguimientos.filter { it.estado == estado }

    /** Lista los seguimientos cuyo título pertenece a un [genero]. */
    fun porGenero(genero: Genero): List<Seguimiento> =
        _seguimientos.filter { genero in it.titulo.generos }

    /** Recomendaciones según los géneros favoritos del usuario. */
    fun recomendaciones(catalogo: List<TituloAudiovisual>): List<TituloAudiovisual> {
        TODO("Fase 2: recomendar según géneros de títulos mejor puntuados")
    }

    override fun toString(): String = "Usuario(id=$id, nombre='$nombre')"
}

// ---------------------------------------------------------------------------
// Demostración
// ---------------------------------------------------------------------------

fun main() {
    // Tipos y géneros
    val tv = Tipo("TV")
    val mangaSerializado = Tipo("Manga")
    val accion = Genero("Acción")
    val fantasia = Genero("Fantasía")
    val romance = Genero("Romance")

    // Títulos (subclases concretas de la clase abstracta)
    val frieren = Anime(
        id = 1, nombre = "Sousou no Frieren", tipo = tv,
        generos = setOf(fantasia, accion),
        sinopsis = "Una maga elfa reflexiona sobre el tiempo tras derrotar al Rey Demonio.",
        episodios = 28, estudio = "Madhouse"
    )
    val berserk = Manga(
        id = 2, nombre = "Berserk", tipo = mangaSerializado,
        generos = setOf(accion, fantasia),
        capitulos = 375, autor = "Kentaro Miura"
    )
    berserk.agregarGenero(romance) // N —— N: se pueden agregar géneros después

    // Usuario y seguimientos (clase asociativa)
    val ana = Usuario(id = 1, nombre = "Ana")
    val seguimientoFrieren = ana.seguir(frieren, EstadoSeguimiento.VIENDO)
    val seguimientoBerserk = ana.seguir(berserk)

    seguimientoFrieren.avanzar(10)
    seguimientoFrieren.puntuar(10)
    seguimientoFrieren.alternarFavorito()
    seguimientoBerserk.avanzar(5)

    // Demostración de relaciones
    println("== Catálogo ==")
    listOf(frieren, berserk).forEach { println(it.descripcionCorta()) }

    println("\n== Seguimientos de ${ana.nombre} (Usuario 1 —— N Seguimiento) ==")
    ana.seguimientos.forEach(::println)

    println("\n== Seguimientos de '${frieren.nombre}' (Título 1 —— N Seguimiento) ==")
    frieren.seguimientos.forEach(::println)

    println("\n== Géneros de '${berserk.nombre}' (Título N —— N Género) ==")
    println(berserk.generos.joinToString { it.nombre })

    println("\n== Favoritos de ${ana.nombre} ==")
    println(ana.favoritos.joinToString { it.nombre })

    println("\n== Viendo ahora ==")
    ana.porEstado(EstadoSeguimiento.VIENDO).forEach { println(it.titulo.nombre) }

    println("\nPuntuación promedio de ${frieren.nombre}: ${frieren.puntuacionPromedio ?: "sin puntuar"}")
}

/*
 * Diagrama de relaciones
 *
 *   +---------+ 1      N +--------------+ N      1 +----------------------+
 *   | Usuario |----------| Seguimiento  |----------| TituloAudiovisual    |
 *   +---------+          | (asociativa) |          | {abstracta}          |
 *                        | estado       |          +----------------------+
 *                        | progreso     |            ^        |        |
 *                        | puntuacion   |            |        | N      | N
 *                        | favorito     |      +-----+-----+  |        |
 *                        +--------------+      |           |  | 1      | N
 *                                           +-------+ +-------+ +------+ +--------+
 *                                           | Anime | | Manga | | Tipo | | Genero |
 *                                           +-------+ +-------+ +------+ +--------+
 *
 *   TituloAudiovisual N —— 1 Tipo
 *   TituloAudiovisual N —— N Genero
 */
