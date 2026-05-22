package id.ac.ui.cs.advprog.mysawitbe.modules.panen.domain;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PanenPhotoTest {

    // =========================================================================
    // create() — factory method
    // =========================================================================
    @Nested
    class Create {

        @Test
        void shouldCreatePhotoWithCorrectUrl() {
            String url = "https://cdn.example.com/foto.jpg";

            PanenPhoto photo = PanenPhoto.create(url);

            assertEquals(url, photo.getPhotoUrl());
        }

        @Test
        void shouldGenerateNonNullId() {
            PanenPhoto photo = PanenPhoto.create("https://cdn.example.com/foto.jpg");

            assertNotNull(photo.getId());
        }

        @Test
        void shouldGenerateUniqueIdForEachPhoto() {
            PanenPhoto photo1 = PanenPhoto.create("https://cdn.example.com/foto1.jpg");
            PanenPhoto photo2 = PanenPhoto.create("https://cdn.example.com/foto2.jpg");

            assertNotEquals(photo1.getId(), photo2.getId());
        }

        @Test
        void shouldGenerateUniqueIdEvenForSameUrl() {
            String url = "https://cdn.example.com/foto.jpg";

            PanenPhoto photo1 = PanenPhoto.create(url);
            PanenPhoto photo2 = PanenPhoto.create(url);

            assertNotEquals(photo1.getId(), photo2.getId());
        }
    }

    // =========================================================================
    // Constructor langsung (untuk reconstruct dari persistence)
    // =========================================================================
    @Nested
    class DirectConstructor {

        @Test
        void shouldStoreGivenIdAndUrl() {
            UUID id  = UUID.randomUUID();
            String url = "https://cdn.example.com/foto.jpg";

            PanenPhoto photo = new PanenPhoto(id, url);

            assertEquals(id,  photo.getId());
            assertEquals(url, photo.getPhotoUrl());
        }
    }
}