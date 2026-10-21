package com.ksiracare.backend.policy;

import com.ksiracare.backend.enums.Language;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Which aapta can take which client: they must share a language. A client who named no languages
 * (bookings made before languages were asked for) can be taken by anyone.
 */
@Component
public class AaptaMatchPolicy {

    public boolean canTake(Set<Language> aaptaLanguages, Set<Language> clientLanguages) {
        return clientLanguages.isEmpty() || !Collections.disjoint(aaptaLanguages, clientLanguages);
    }

    /**
     * Whether every client can have an aapta of their own who speaks one of their languages — a
     * maximum bipartite matching (augmenting paths), so one Hindi speaker isn't counted twice for
     * two Hindi-only clients while an English-only aapta sits free. Sizes are an hour's bookings
     * and open aaptas, so this is instant.
     *
     * @param clients each client's languages
     * @param aaptas  each open aapta's languages
     */
    public boolean canSeatAll(List<Set<Language>> clients, List<Set<Language>> aaptas) {
        if (clients.size() > aaptas.size()) {
            return false;
        }
        int[] clientOfAapta = new int[aaptas.size()];
        Arrays.fill(clientOfAapta, -1);
        for (int client = 0; client < clients.size(); client++) {
            if (!seat(client, clients, aaptas, clientOfAapta, new boolean[aaptas.size()])) {
                return false;
            }
        }
        return true;
    }

    /** Finds an aapta for {@code client}, moving earlier clients to other aaptas if that frees one. */
    private boolean seat(int client, List<Set<Language>> clients, List<Set<Language>> aaptas,
                         int[] clientOfAapta, boolean[] tried) {
        for (int aapta = 0; aapta < aaptas.size(); aapta++) {
            if (tried[aapta] || !canTake(aaptas.get(aapta), clients.get(client))) {
                continue;
            }
            tried[aapta] = true;
            if (clientOfAapta[aapta] == -1 || seat(clientOfAapta[aapta], clients, aaptas, clientOfAapta, tried)) {
                clientOfAapta[aapta] = client;
                return true;
            }
        }
        return false;
    }
}
