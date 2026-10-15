# AniList

Catálogo unificado de productos audiovisuales (anime, manga, series, películas) con seguimiento por usuario.
Bosquejo en **Kotlin puro / JVM**: sin Android SDK ni frameworks externos, solo la stdlib de Kotlin.

> Estado: bosquejo (fase 1). Pensado para migrar después a Android (Compose, Room).

## Modelo

| Clase | Tipo | Rol |
|-------|------|-----|
| `Usuario` | clase | Gestiona sus seguimientos |
| `TituloAudiovisual` | abstracta | Base de `Anime` y `Manga` |
| `Tipo`, `Genero` | data class | Categorización (N—1 y N—N) |
| `Seguimiento` | data class (asociativa) | Usuario ↔ Título: estado, progreso, puntuación, favorito |
| `EstadoSeguimiento` | enum | PENDIENTE, VIENDO, COMPLETADO, ABANDONADO |

Diagrama de clases: ![imagen.png](https://raw.githubusercontent.com/parzibyte/WaterPy/master/assets/ImagenV1.png)

## Requisitos

- JDK 17 o superior.
- Gradle 8.x, o simplemente IntelliJ IDEA (trae soporte para Gradle).
- Internet la primera vez, para descargar el plugin de Kotlin.

## Uso

```bash
gradle run     # ejecuta la demostración (main)
gradle test    # corre las pruebas unitarias
gradle build   # compila y prueba todo
```

En IntelliJ IDEA: *File → Open* y elegir la carpeta del repositorio.

Opcional: para usar el Gradle Wrapper, ejecuta `gradle wrapper` una vez y sube `gradlew`, `gradlew.bat` y `gradle/wrapper/` al repositorio.

## Estructura

```
.
├── .github/workflows/ci.yml            # CI de GitHub Actions
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── docs/AniList.puml
└── src/
    ├── main/kotlin/anilist/AniList.kt
    └── test/kotlin/anilist/AniListTest.kt
```

## Próximos pasos

- Separar en módulos `domain` y `data` con interfaces de repositorio.
- Persistencia con Room (tabla intermedia Título–Género; `Seguimiento` con clave compuesta).
- Modelo inmutable con `StateFlow` + `ViewModel` para Compose.
- Más subclases (`Pelicula`, `Serie`) y búsqueda/filtros por género y tipo.
- Implementar `Usuario.recomendaciones()`.

## Licencia

[MIT](LICENSE)
