[TOC]



## 1. Mapeo JPA



### Mapeo de tipos primitivos
Suele ser prefefible mapear atributos cuyo tipo sea uno de los tipos primitivos de Java [`int, float, double, byte, boolean`, etc] empleando los correspondientes tipos Objeto (_wrappers_) [`Integer, Float, Double, Byte, Boolean`, etc]  

* Ambos opciones son válidas, pero los valores de tipo Objeto admiten el valor _null_ (y otros valores especiales [no inicializado, infinito, ...]) que pueden ser necesarios en columnas de la BD que admitan nulos, mientras que los tipos primitivos no permiten esa opción
* Sólo serían recomendables los tipos primitivos por cuestiones de eficiencia y siempre sobre columnas que NO admitan nulos.
* En el caso de cantidades numéricas de tipo flotante que representen importes en moneda u otro tipo de valores donde la pérdida de precisión o los efectos del redondeo sean relevantes, se recomienda mapear esas columnas empleando datos de tipo BigDecimal, que son objetos que encapsulan cantidades numéricas de precisión arbitraria sin pérdida. 

### Relaciones bidireccionales vs. unidireccionales

El mapeo de las relaciones depende de los casos de uso a los que se pretenda dar soporte ( ver detalles en [JPA Relationships](https://en.wikibooks.org/wiki/Java_Persistence/Relationships)).

* Como regla general, si no hay casos de uso que justifiquen implementar relaciones bidireccionales, suele ser  recomendable el mapeo en un único sentido (normalmente en el lado `ManyToOne` ó `OnetoOne` de las relaciones 1:N y 1:1, si es posible), resolviendo el acceso a los datos en el otro sentido con Queries cuando sea necesario.
* En el caso de realizar el mapeo bidireccional es necesario asegurar la consistencia de los datos en ambos sentidos de la relación, especialmente se si han definido operaciones en cascada. (Ver en [bidirectiona association pattern](https://xebia.com/blog/jpa-implementation-patterns-bidirectional-assocations/) y [JPA relationship](https://en.wikibooks.org/wiki/Java_Persistence/Relationships#Object_corruption,_one_side_of_the_relationship_is_not_updated_after_updating_the_other_side) un patrón  de establecimiento coordinado de los enlaces en ambos sentidos en relaciones bidireccionales 1:N)


#### Uso de `mappedBy` y `@JoinColumn` en relaciones 1:1 y 1:N
El atributo `mappedBy` de las relaciones `OneToOne` y `OneToMany` sirve para indicar en la **entidad que no posee la clave foránea** a qué atributo de la entidad al otro lado de la relación se vincula la clave foránea (dicho atributo debe estar anotado con `@OnetoOne` ó `ManytoOne`).

* Si en una de estas relaciones no se indica el atributo `mappedBy` el motor JPA resolverá la relación creando una tabla intermedia, en lugar de hacerlo mediante una clave foránea en la entidad relacionada. 

La anotación `@JoinColumn` es opcional y sirve para indicar información de la columna que hace el papel de clave foránea de la relación (nombre de la clave foránea, nombre de la columna 'apuntada' en el otro lado de la relación, etc).

* Normalmente se indica en la _entidad portadora_ de la clave foránea (lado `@ManytoOne` en 1:N y en el lado `OneToOne` donde se desea ubicar esa clave foránea en 1:1). 
* La especificación JPA contempla que se use `@JoinColumn` en la entidad que no porta la clave foránea únicamente en los caos en los que la relación no se mapea desde la otra entidad, precisamente para indicar el nombre de esa columna _clave foránea_ en la entidad al otro lado de la relación.



### Operaciones en cascada y `orphanRemoval`
El parámetro `cascade` sirve para indicar qué operaciones (_persist_, _merge_, _remove_, etc) realizadas sobre una entidad se propagan a las entidades relacionadas con ella a través de la relación donde se establece este parámetro (ver [JPA cascading](https://en.wikibooks.org/wiki/Java_Persistence/Relationships#Cascading)).

* Por defecto todas las operaciones en cascada están deshabilitadas, habilitar estas operaciones depende de los casos de uso que se pretendan implementar. 
* Al trabajar con relaciones bidireccionales puede no tener sentido aplicar las operaciones en cascada en ambos sentidos (especialmente la operación de borrado).
* En relaciones mapeadas en ambas direcciones es importante que los datos de los enlaces en ambos sentidos sean consistentes para que las operaciones en cascada no fallen.

`orphanRemoval` es un parámetro complementario a `cascade`, necesario para que las operaciones `cascade.MERGE` funcionen correctamente.

* Con `cascade.REMOVE`, cuando se elimina una instancia de una entidad, se eliminan las instancias relacionadas con ella.
* Con `cascade.MERGE`, cuando se modifica una instancia de una entidad:
     * si se han añadido instancias relacionas al otro lado de la relación, estas se crearán con los `insert` de SQL necesarios
     * si se ha modificado alguna de las instancias relacionadas al otro lado de la relación, esta será actualizada con un `update` de SQL
     * si se han eliminado instancias relacionadas al otro lado de la relación, estás se eliminarán con  `delete` de SQL **SOLO** sí `orphanRemoval` se establece a `true` (por defecto es `false` y esas eliminaciones en la relación no se traducirán en comandos `delete` sobre la Base de Datos)    


### Lazy vs Eager
En general se recomienda evitar mapear las relaciones con `FetchType.EAGER`, dado que supone una carga de trabajo sobre la Base de Datos (operaciones `JOIN`) y, posiblemente, el movimiento de grandes cantidades de datos que en muchos casos no se van a utilizar.

* Sólo se justificaría su uso para cubir casos de uso donde una entidad y sus instancias relacionadas se usen de forma conjunta habitualmente y siempre que el número de instancias relacionadas no sea excesivo.

Como norma general, el mapeo por defecto suele ser una opción adecuada:

* `Fetch.Type.EAGER` para relaciones `@ManyToOne` y `@OneToOne`
* `Fetch.Type.LAZY` para relaciones `@ManyToMany` y `OneToMany`

Siempre es posible resolver los casos de uso que saquen provecho del acceso `EAGER` empleando queries con la opción JOIN FETCH en su claúsula WHERE (ver (https://en.wikibooks.org/wiki/Java_Persistence/JPQL#JOIN_FETCH)) que fuerza la carga de las relaciones en el momento de la consulta.

El acceso a relaciones mapeadas usando `FetchType.LAZY` sólo funcionará si se realiza dentro del contexto de persistencia activo (dentro de una transacción con contextos de persistencia ligados a las transacciones o antes de invocar `EntityManager.close()` si se gestiona el contexto de persintencia manualmente).

* Este tipo de accesos _LAZY_ fuera del contexto de persistencia (por ejemplo desde un objeto de la capa web) fallará, provocando una excepción`LazyInitializationException`.

## 2. Uso de SPRING



### Repositorios Spring DATA JPA

`<pendiente>`

### Arquitectura general de la capa de servicios

`<pendiente>`

### Consideraciones REST y @RestController

`<pendiente>`

