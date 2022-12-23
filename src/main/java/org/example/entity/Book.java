package org.example.entity;

import org.example.lib.annotation.*;

import java.time.LocalDate;
import java.util.StringJoiner;

@Entity
@Table(name = "book")
public class Book {
    @Id
    private Long id;
    @Column()
    private String title;
    @Column(name = "published_at")
    private LocalDate publishedAt;

    // 2nd stage:
    @ManyToOne()
    Publisher publisher = null;

    public Book() {
    }

    public Book(String title, LocalDate publishedAt) {
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Book.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("title='" + title + "'")
                .add("publishedAt=" + publishedAt)
                .add("publisher=" + publisher)
                .toString();
    }
}
