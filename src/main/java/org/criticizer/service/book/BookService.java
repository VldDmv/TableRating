package org.criticizer.service.book;

import java.util.HashSet;
import java.util.List;
import org.criticizer.dto.book.BookResponse;
import org.criticizer.dto.genre.GenreResponse;
import org.criticizer.entity.Book;
import org.criticizer.entity.Genre;
import org.criticizer.entity.MediaStatus;
import org.criticizer.repository.BookRepository;
import org.criticizer.repository.GenreRepository;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.ServiceValidator;
import org.springframework.stereotype.Service;

@Service
public class BookService extends AbstractMediaService<Book, BookResponse> {

    private final GenreRepository genreRepository;

    public BookService(
            BookRepository bookRepository,
            GenreRepository genreRepository,
            ServiceValidator validator) {
        super(bookRepository, validator);
        this.genreRepository = genreRepository;
    }

    @Override
    protected String getEntityName() {
        return "Book";
    }

    @Override
    protected Book createEntity(String name, String coverUrl, Integer userId, Integer score) {
        Book book = new Book(null, name, userId, score, MediaStatus.PLANNED);
        book.setCoverUrl(coverUrl);
        return book;
    }

    @Override
    protected void assignCategories(Book book, List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            book.setGenres(new HashSet<>());
            return;
        }

        List<Genre> genres = genreRepository.findAllById(genreIds);
        book.setGenres(new HashSet<>(genres));
    }

    @Override
    protected BookResponse toResponse(Book book) {
        List<GenreResponse> genreResponses =
                book.getGenres() != null
                        ? book.getGenres().stream().map(GenreResponse::from).toList()
                        : List.of();

        return new BookResponse(
                book.getId(),
                book.getName(),
                book.getCoverUrl(),
                book.getScore(),
                book.getStatus().name(),
                genreResponses);
    }

    @Override
    public String getMediaType() {
        return "books";
    }
}
