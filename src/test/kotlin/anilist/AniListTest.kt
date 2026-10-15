package anilist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AniListTest {

    private val tv = Tipo("TV")
    private val accion = Genero("Acción")

    @Test
    fun `seguir registra el seguimiento en usuario y titulo`() {
        val usuario = Usuario(1, "Ana")
        val anime = Anime(1, "Frieren", tv, setOf(accion), episodios = 28)

        val seguimiento = usuario.seguir(anime)

        assertEquals(listOf(seguimiento), usuario.seguimientos)
        assertEquals(listOf(seguimiento), anime.seguimientos)
    }

    @Test
    fun `no se puede seguir dos veces el mismo titulo`() {
        val usuario = Usuario(1, "Ana")
        val anime = Anime(1, "Frieren", tv, episodios = 28)
        usuario.seguir(anime)

        assertFailsWith<IllegalArgumentException> { usuario.seguir(anime) }
    }

    @Test
    fun `avanzar hasta el total marca como completado`() {
        val usuario = Usuario(1, "Ana")
        val anime = Anime(1, "Corto", tv, episodios = 12)
        val seguimiento = usuario.seguir(anime)

        seguimiento.avanzar(5)
        assertEquals(EstadoSeguimiento.VIENDO, seguimiento.estado)

        seguimiento.avanzar(20)
        assertEquals(12, seguimiento.progreso)
        assertEquals(EstadoSeguimiento.COMPLETADO, seguimiento.estado)
    }

    @Test
    fun `puntuacion fuera de rango falla y el promedio funciona`() {
        val anime = Anime(1, "Frieren", tv, episodios = 28)
        val a = Usuario(1, "Ana").seguir(anime)
        val b = Usuario(2, "Beto").seguir(anime)

        assertNull(anime.puntuacionPromedio)
        assertFailsWith<IllegalArgumentException> { a.puntuar(11) }

        a.puntuar(10)
        b.puntuar(8)
        assertEquals(9.0, anime.puntuacionPromedio)
    }

    @Test
    fun `generos se pueden agregar sin duplicar`() {
        val manga = Manga(2, "Berserk", Tipo("Manga"), setOf(accion), capitulos = 375)

        assertTrue(manga.agregarGenero(Genero("Romance")))
        assertEquals(false, manga.agregarGenero(accion))
        assertEquals(2, manga.generos.size)
    }
}
