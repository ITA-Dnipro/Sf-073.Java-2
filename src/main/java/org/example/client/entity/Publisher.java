package org.example.client.entity;

import org.example.lib.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Entity
@Table(name = "publishers")
public class Publisher {
    @Id
    private Long id;
    @Column()
    private String name;

    @OneToMany(mappedBy = "publishers")
    private List<Book> books;

    public Publisher() {
    }

    public Publisher(String name) {
        this.books = new ArrayList<>();
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

    public List<Book> getBooks() {
        return books;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", Publisher.class.getSimpleName() + "[", "]")
                .add("id = " + id)
                .add("name = '" + name + "'");
        if (books!= null && !books.isEmpty()) {
            sj.add("book ids: ");
            books.forEach(b -> sj.add(String.valueOf(b.getId())));
        }
        return sj.toString();
    }
}
