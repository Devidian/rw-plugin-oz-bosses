package de.omegazirkel.risingworld.bosses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class BossInformantServiceTest {
    @Test
    public void mailSubjectFitsTheSuppliedMailLimitAndRejectsMarkup() {
        String subject = BossInformantService.mailSubject("Kopfgeldjäger-Information: <Ancient Warlord>", 25);

        assertEquals(25, subject.length());
        assertFalse(subject.contains("<"));
        assertFalse(subject.contains(">"));
    }

    @Test
    public void mailSubjectHasAFallbackWhenTranslationIsMissing() {
        assertEquals("Boss report", BossInformantService.mailSubject(null, 50));
    }

    @Test
    public void mailSubjectPreservesLongerSubjectsWhenMailAllowsThem() {
        String subject = "Kopfgeldjäger-Information: Ancient Warlord";

        assertEquals(subject, BossInformantService.mailSubject(subject, 50));
    }
}
