package app.notesr.migration.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

class AppMigrationServiceTest {
    @Test
    void testRunsMigrationsInCorrectOrder() {
        AppMigration migration1 = mock(AppMigration.class);
        AppMigration migration2 = mock(AppMigration.class);

        when(migration1.getFromVersion()).thenReturn(0);
        when(migration1.getToVersion()).thenReturn(1);

        when(migration2.getFromVersion()).thenReturn(1);
        when(migration2.getToVersion()).thenReturn(2);

        AppMigrationService service = new AppMigrationService(List.of(migration1, migration2));
        service.run(0, 2);

        InOrder inOrder = inOrder(migration1, migration2);

        inOrder.verify(migration1).migrate();
        inOrder.verify(migration2).migrate();
    }
}