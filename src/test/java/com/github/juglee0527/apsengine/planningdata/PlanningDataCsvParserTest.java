package com.github.juglee0527.apsengine.planningdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PlanningDataCsvParserTest {

    private final PlanningDataCsvParser parser =
            new PlanningDataCsvParser();

    @Test
    void parsesUtf8BomQuotedCommaAndEscapedQuote() {
        String csv = "\ufeff"
                + PlanningDataCsvTestSupport.header()
                + "\r\n"
                + PlanningDataCsvTestSupport.row(Map.of(
                        "type", "FACTORY",
                        "factoryCode", "factory-01",
                        "name", "서울, \"1공장\""
                ));

        PlanningDataCsvParser.ParsedCsv parsed = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8),
                10
        );

        assertThat(parsed.rows()).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(2);
            assertThat(row.fields().get("factoryCode"))
                    .isEqualTo("factory-01");
            assertThat(row.fields().get("name"))
                    .isEqualTo("서울, \"1공장\"");
        });
    }

    @Test
    void rejectsUnclosedQuote() {
        String csv = PlanningDataCsvTestSupport.header()
                + "\n\"FACTORY";

        assertThatThrownBy(() -> parser.parse(
                csv.getBytes(StandardCharsets.UTF_8),
                10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("닫히지 않은 따옴표");
    }

    @Test
    void keepsColumnCountMismatchAsRowErrorInput() {
        String csv = PlanningDataCsvTestSupport.header()
                + "\nFACTORY,F-01";

        PlanningDataCsvParser.ParsedCsv parsed = parser.parse(
                csv.getBytes(StandardCharsets.UTF_8),
                10
        );

        assertThat(parsed.rows()).singleElement().satisfies(row -> {
            assertThat(row.fields()).isEmpty();
            assertThat(row.actualColumnCount()).isEqualTo(2);
        });
    }
}
