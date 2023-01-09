package org.example.client.entity;

import org.example.lib.annotation.*;

import java.util.*;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id
    private Long id;
    @Column()
    private String name;

    @OneToMany(mappedBy = "publishers")
    private Set<Book> books;

    public Publisher() {
    }

    public Publisher(String name) {
        this.books = new HashSet<>();
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Book> getBooks() {
        return books;
    }

    public void setBooks(Set<Book> books) {
        this.books = books;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", Publisher.class.getSimpleName() + "[", "]")
                .add("id = " + id)
                .add("name = '" + name + "'");
        if (books!= null && books.size() > 0 && getId() != null) {
            sj.add("book ids: ");
            books.forEach(b -> sj.add(b.getId().toString()));
        }
        return sj.toString();
    }
}
