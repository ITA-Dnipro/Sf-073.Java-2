package org.example.client.entity;

import org.example.lib.annotation.*;

import java.time.LocalDate;
import java.util.StringJoiner;

@Entity
@Table(name = "books")
public class Book {
    @Id
    private Long id;
    @Column()
    private String title;
    @Column(name = "published_at")
    private LocalDate publishedAt;
    @ManyToOne(columnName = "publisher_id")
    private Publisher publisher;

    public Book() {
    }

    public Book(String title, LocalDate publishedAt) {
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public Book(String title, LocalDate publishedAt, Publisher publisher) {
        this(title, publishedAt);
        this.publisher = publisher;
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

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", Book.class.getSimpleName() + "[", "]")
                .add("id = " + id)
                .add("title = '" + title + "'")
                .add("publishedAt = " + publishedAt);
        if (publisher != null) {
            sj.add("[publisherId = " + publisher.getId());
            sj.add("publisherName = " + publisher.getName());
            if (publisher.getBooks() != null) {
                sj.add("book ids: ");
                publisher.getBooks().forEach(b -> {
                    if (b.getId() != null) {
                        sj.add(b.id.toString());
                    }
                });
            }
        }
        return sj.toString();
    }
}
