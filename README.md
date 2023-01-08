# Sf-073.Java-2
Mini ORM project for demonstration working with H2 database and @OneToMany  / @ManyToOne relationships.

Functionality:
•	void register(Class<?>... entityClasses);

•	<T> T save(T o);

•	void persist(Object o);

•	<T> Optional<T> findById(Serializable id, Class<T> cls);

•	<T> List<T> findAll(Class<T> cls);

•	<T> T merge(T o);

•	<T> T refresh(T o);

•	boolean delete(Object o);
