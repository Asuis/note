

先说说JPA和Hibernate的关系

JPA（Java Persistence API），是Java EE 5的标准ORM接口，也是ejb3规范的一部分。

Hibernate是当今很流行的ORM框架，也是JPA的一个实现，其它还有Toplink之类的ROM框架。

JPA和Hibernate之间的关系，可以简单的理解为JPA是标准接口，Hibernate是实现。
Hibernate主要是通过三个组件来实现的：

    hibernate-core：Hibernate的核心实现，提供了Hibernate所有的核心功能。
    hibernate-entitymanager：Hibernate实现了标准的JPA，可以把它看成hibernate-core和JPA之间的适配器，它并不直接提供ORM的功能，而是对hibernate-core进行封装，使得Hibernate符合JPA的规范。
    hibernate-annotation：Hibernate支持annotation方式配置的基础，它包括了标准的JPA annotation以及Hibernate自身特殊功能的annotation。

注解详细如下：
1
@Entity(name="EntityName")

必须，name为可选，对应数据库中一的个表
1
	
@Table(name="",catalog="",schema="")

可选，通常和 @Entity 配合使用，只能标注在实体的class定义处,表示实体对应的数据库表的信息。
name：可选，表示表的名称。默认，表名和实体名称一致，只有在不一致的情况下才需要指定表名。
catalog：可选，表示Catalog名称，默认为Catalog(“”)。
schema：可选，表示Schema名称，默认为Schema(“”)。

 

@id
@id定义了映射到数据库表的主键的属性，一个实体只能有一个属性被映射为主键。

@GeneratedValue(strategy=GenerationType,generator=””)
可选
strategy：表示主键生成策略，有AUTO，INDENTITY，SEQUENCE 和 TABLE 4种，分别表示让ORM框架自动选择，根据数据库的 Identity 字段生成，根据数据库表的 Sequence 字段生成，以有根据一个额外的表生成主键，默认为 AUTO。

generator:表示主键生成器的名称，这个属性通常和ORM框架相关，例如，Hibernate可以指定uuid等主键生成方式.

@Basic(fetch=FetchType,optional=true)
可选
@Basic表示一个简单的属性到数据库表的字段的映射，对于没有任何标注的getXxxx()方法，默认即为@Basic
fetch：表示该属性的读取策略，有EAGER和LAZY两种，分别表示主支抓取和延迟加载，默认为EAGER。
optional：表示该属性是否允许为null，默认为true。

@Column
可选
@Column描述了数据库表中该字段的详细定义，这对于根据JPA注解生成数据库表结构的工具非常有作用。
name：表示数据库表中该字段的名称，默认情形属性名称一致。
nullable：表示该字段是否允许为null，默认为true。
unique：表示该字段是否是唯一标识，默认为false。
length：表示该字段的大小，仅对String类型的字段有效。
insertable：表示在ORM框架执行插入操作时，该字段是否应出现INSETRT语句中，默认为true。
updateable：表示在ORM框架执行更新操作时，该字段是否应该出现在UPDATE语句中，默认为true。对于一经创建就不可以更改的字段，该属性非常有用，如对于birthday字段。
columnDefinition：表示该字段在数据库中的实际类型。通常ORM框架可以根据属性类型自动判断数据库中字段的类型，但是对于Date类型仍无法确定数据库中字段类型究竟是DATE，TIME还是TIMESTAMP。此外，String的默认映射类型为VARCHAR，如果要将String类型映射到特定数据库的BLOB或TEXT字段类型，该属性非常有用。

@Transient
可选
@Transient表示该属性并非一个到数据库表的字段的映射，ORM框架将忽略该属性。
如果一个属性并非数据库表的字段映射。就务必将其标示为@Transient。否则。ORM框架默认其注解为@Basic

 @OneToOne(fetch=FetchType,cascade=CascadeType)
可选
@OneToOne描述一个一对一的关联
fetch：表示抓取策略，默认为FetchType.LAZY
cascade：表示级联操作策略

@ManyToOne(fetch=FetchType,cascade=CascadeType)
可选
@ManyToOne表示一个多对一的映射,该注解标注的属性通常是数据库表的外键
optional：是否允许该字段为null，该属性应该根据数据库表的外键约束来确定，默认为true
fetch：表示抓取策略，默认为FetchType.EAGER
cascade：表示默认的级联操作策略，可以指定为ALL，PERSIST，MERGE，REFRESH和REMOVE中的若干组合，默认为无级联操作
targetEntity：表示该属性关联的实体类型。该属性通常不必指定，ORM框架根据属性类型自动判断targetEntity。

 

@OneToMany(fetch=FetchType,cascade=CascadeType)
可选
@OneToMany描述一个一对多的关联,该属性应该为集体类型,在数据库中并没有实际字段。
fetch：表示抓取策略,默认为FetchType.LAZY,因为关联的多个对象通常不必从数据库预先读取到内存
cascade：表示级联操作策略,对于OneToMany类型的关联非常重要,通常该实体更新或删除时,其关联的实体也应当被更新或删除
例如：实体User和Order是OneToMany的关系，则实体User被删除时，其关联的实体Order也应该被全部删除

@ManyToMany
可选

@ManyToMany 描述一个多对多的关联.多对多关联上是两个一对多关联,但是在ManyToMany描述中,中间表是由ORM框架自动处理
targetEntity:表示多对多关联的另一个实体类的全名,例如:package.Book.class
mappedBy:表示多对多关联的另一个实体类的对应集合属性名称
两个实体间相互关联的属性必须标记为@ManyToMany,并相互指定targetEntity属性,
需要注意的是,有且只有一个实体的@ManyToMany注解需要指定mappedBy属性,指向targetEntity的集合属性名称
利用ORM工具自动生成的表除了User和Book表外,还自动生成了一个User_Book表,用于实现多对多关联

@JoinColumn
可选
@JoinColumn和@Column类似,介量描述的不是一个简单字段,而一一个关联字段,例如.描述一个@ManyToOne的字段.
name:该字段的名称.由于@JoinColumn描述的是一个关联字段,如ManyToOne,则默认的名称由其关联的实体决定.
例如,实体Order有一个user属性来关联实体User,则Order的user属性为一个外键,
其默认的名称为实体User的名称+下划线+实体User的主键名称

@JoinTable(name = “student_teacher”, inverseJoinColumns = @JoinColumn(name = “tid”), joinColumns = @JoinColumn(name = “sid”))

可选

由第三张表来维护两张表的关系

name：是关系表的名字

joinColumns：自己这一端的主键

inverseJoinColumns：对方的主键

 

@MappedSuperclass
可选
@MappedSuperclass可以将超类的JPA注解传递给子类,使子类能够继承超类的JPA注解

@Embedded
@Embedded将几个字段组合成一个类,并作为整个Entity的一个属性.
例如User包括id,name,city,street,zip属性.
我们希望city,street,zip属性映射为Address对象.这样,User对象将具有id,name和address这三个属性.
Address对象必须定义为@Embededable
验证注解

@Pattern
String
通过正则表达式来验证字符串
@Pattern(regex=”[a-z]{6}”)

@Length
String
验证字符串的长度
@length(min=3,max=20)

@Email
String
验证一个Email地址是否有效
@email

@Range
Long
验证一个整型是否在有效的范围内
@Range(min=0,max=100)

@Min
Long
验证一个整型必须不小于指定值
@Min(value=10)

@Max
Long
验证一个整型必须不大于指定值
@Max(value=20)

@Size
集合或数组
集合或数组的大小是否在指定范围内
@Size(min=1,max=255)
 
 
摘自：
http://www.cnblogs.com/luoxiaolei/p/4272494.html
