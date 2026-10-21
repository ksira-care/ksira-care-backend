package com.ksiracare.backend.policy;

import com.ksiracare.backend.enums.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.ksiracare.backend.enums.Language.ENGLISH;
import static com.ksiracare.backend.enums.Language.HINDI;
import static com.ksiracare.backend.enums.Language.KANNADA;
import static com.ksiracare.backend.enums.Language.MARATHI;
import static org.assertj.core.api.Assertions.assertThat;

class AaptaMatchPolicyTest {

    private final AaptaMatchPolicy policy = new AaptaMatchPolicy();

    @Test
    void anAaptaCanTakeAClientWhoSharesALanguage() {
        assertThat(policy.canTake(Set.of(ENGLISH, HINDI), Set.of(HINDI, MARATHI))).isTrue();
        assertThat(policy.canTake(Set.of(ENGLISH), Set.of(KANNADA))).isFalse();
    }

    @Test
    void aClientWhoNamedNoLanguageCanBeTakenByAnyone() {
        assertThat(policy.canTake(Set.of(KANNADA), Set.of())).isTrue();
    }

    @Test
    void eachClientNeedsAnAaptaOfTheirOwn() {
        List<Set<Language>> oneHindiSpeaker = List.of(Set.of(HINDI), Set.of(ENGLISH));

        assertThat(policy.canSeatAll(List.of(Set.of(HINDI)), oneHindiSpeaker)).isTrue();
        assertThat(policy.canSeatAll(List.of(Set.of(HINDI), Set.of(HINDI)), oneHindiSpeaker)).isFalse();
        assertThat(policy.canSeatAll(List.of(Set.of(HINDI), Set.of(ENGLISH)), oneHindiSpeaker)).isTrue();
    }

    @Test
    void earlierClientsMoveAsideWhenThatSeatsEveryone() {
        // The Marathi client could sit with either aapta; only the first speaks Hindi.
        List<Set<Language>> aaptas = List.of(Set.of(HINDI, MARATHI), Set.of(MARATHI));
        List<Set<Language>> clients = List.of(Set.of(MARATHI), Set.of(HINDI));

        assertThat(policy.canSeatAll(clients, aaptas)).isTrue();
    }

    @Test
    void moreClientsThanAaptasNeverFit() {
        assertThat(policy.canSeatAll(List.of(Set.of(), Set.of()), List.of(Set.of(HINDI)))).isFalse();
        assertThat(policy.canSeatAll(List.of(), List.of())).isTrue();
    }
}
