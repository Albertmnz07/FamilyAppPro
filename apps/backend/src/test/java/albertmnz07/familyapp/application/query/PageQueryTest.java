package albertmnz07.familyapp.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import albertmnz07.familyapp.domain.exception.InvalidValueException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void pageQueryClampsAboveMaximumAndNormalizesFactoryInputs() {
        assertThat(PageQuery.of(-3, 0)).isEqualTo(new PageQuery(0, PageQuery.DEFAULT_SIZE));
        assertThat(PageQuery.of(2, 500)).isEqualTo(new PageQuery(2, PageQuery.MAX_SIZE));

        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void pagedResultSlicesOrderedItemsComputesTotalPagesAndCopiesContentDefensively() {
        List<String> source = new ArrayList<>(List.of("a", "b", "c"));

        PagedResult<String> firstPage = PagedResult.paginate(source, PageQuery.of(0, 2));
        PagedResult<String> secondPage = PagedResult.paginate(source, PageQuery.of(1, 2));
        PagedResult<String> beyondEnd = PagedResult.paginate(source, PageQuery.of(5, 2));
        source.add("d");

        assertThat(firstPage.content()).containsExactly("a", "b");
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).containsExactly("c");
        assertThat(beyondEnd.content()).isEmpty();
        assertThatThrownBy(() -> firstPage.content().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
