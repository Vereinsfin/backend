package app.hopps.org.jpa;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BommelTest {

    @Inject
    BommelRepository repo;

    @BeforeEach
    @Transactional
    void clearDatabase() {
        repo.deleteAll();
    }

    @Test
    @TestTransaction
    void simpleGetParentsTest() {
        // Arrange
        var existingBommels = setupSimpleTree();
        var expectedParentsList = List.of(existingBommels.get(1), existingBommels.getFirst());

        // Act
        var actualParentsList = repo.getParentsRecursiveQuery(existingBommels.get(3));

        // Assert
        assertEquals(expectedParentsList, actualParentsList);
    }

    @Test
    @TestTransaction
    void getRoot() {
        // Arrange
        List<Bommel> existingBommels = setupSimpleTree();

        // Act
        Bommel actual = repo.getRoot();

        // Assert
        Bommel expected = existingBommels.getFirst();

        assertNotNull(actual);
        assertEquals(expected, actual);
    }

    @Test
    @TestTransaction
    void simpleInsertionTest() {
        // Arrange
        setupSimpleTree();
        Bommel root = repo.getRoot();

        Bommel newChild = new Bommel();
        newChild.setName("New child");
        newChild.setEmoji("\uD83C\uDF77");
        newChild.setParent(root);

        // Act
        repo.insertBommel(newChild);
        repo.flush();

        // Assert
        repo.getEntityManager().refresh(root);
        assertEquals(3, root.getChildren().size());
        assertTrue(root.getChildren().contains(newChild));

        repo.ensureConsistency();
    }

    @Test
    @TestTransaction
    void disallowTwoRoots() {
        // Arrange
        setupSimpleTree();

        Bommel fakeRoot = new Bommel();
        fakeRoot.setName("I'm a root for sure trust me");

        // Act
        assertThrows(
            IllegalStateException.class,
            () -> repo.createRoot(fakeRoot)
        );

        // Assert
        repo.ensureConsistency();
    }

    @Test
    @DisplayName("Do not allow accidentally creating a new root with the standard insert method")
    void disallowCreatingNewRootWithNormalInsertMethod() {
        // Arrange
        setupSimpleTree();

        Bommel accidentalRoot = new Bommel();
        accidentalRoot.setName("Oops");

        // Act
        assertThrows(
                IllegalArgumentException.class,
                () -> repo.insertBommel(accidentalRoot)
        );

        // Assert
        repo.ensureConsistency();
    }

    @Test
    @TestTransaction
    void disallowAccidentalRecursiveDelete() {
        var bommels = setupSimpleTree();
        var toBeDeleted = bommels.get(1);
        assertEquals(1, toBeDeleted.getChildren().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> QuarkusTransaction.requiringNew().run(
                        () -> repo.deleteBommel(toBeDeleted, false)
                )
        );

        repo.ensureConsistency();
    }

    @Test
    @TestTransaction
    void deletionWorks() {
        // Arrange
        var bommels = setupSimpleTree();

        // Act
        repo.deleteBommel(bommels.get(2), false);

        // Assert
        assertEquals(3, repo.count());
    }

    @Test
    @TestTransaction
    void recursiveDeletionWorks() {
        // Arrange
        var bommels = setupSimpleTree();

        // Act
        repo.deleteBommel(bommels.get(1), true);

        // Assert
        assertEquals(2, repo.count());
    }

    @Test
    @TestTransaction
    void ensureConsistencyDetectsCycle() {
        // Arrange
        var bommel1 = new Bommel();
        bommel1.setName("Bommel1");

        var bommel2 = new Bommel();
        bommel2.setName("Bommel2");

        var bommel3 = new Bommel();
        bommel3.setName("Bommel3");

        bommel1.setParent(bommel2);
        bommel2.setParent(bommel3);
        bommel3.setParent(bommel1);

        // Scary!
        repo.persist(bommel1, bommel2, bommel3);

        // Act + Assert
        assertThrows(
                IllegalStateException.class,
                () -> repo.ensureConsistency()
        );
    }

    @Test
    @TestTransaction
    void ensureConsistencyDetectsMultipleRoots() {
        // Arrange
        setupSimpleTree();

        var secondRoot = new Bommel();
        secondRoot.setName("illegal second root");

        // Scary
        repo.persist(secondRoot);

        // Act + Assert
        assertThrows(
                IllegalStateException.class,
                () -> repo.ensureConsistency()
        );
    }

    @Test
    @TestTransaction
    void simpleBommelMoveWorks() {
        // Arrange
        var bommels = setupSimpleTree();
        // Refetch child from database to make sure its managed
        Bommel child = repo.findById(bommels.get(3).id);
        Bommel parent = bommels.get(2);

        // Act
        repo.moveBommel(child, parent);
        repo.flush();

        // Assert
        var newParent = repo.findById(parent.id);
        repo.getEntityManager().refresh(newParent);
        assertEquals(1, newParent.getChildren().size());
        assertEquals(Set.of(child),  newParent.getChildren());
        assertEquals(4, repo.count());
    }

    /**
     * @return The bommels created
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    List<Bommel> setupSimpleTree() {
        var bommels = generateSimpleTree();

        repo.persist(bommels);
        repo.flush();

        for (var bommel : bommels) {
            repo.getEntityManager().refresh(bommel);
        }

        return bommels;
    }

    private static List<Bommel> generateSimpleTree() {
        Bommel root = new Bommel();
        root.setName("Root bommel");
        root.setEmoji("\uD83C\uDFF3\uFE0F\u200D⚧\uFE0F");

        Bommel child1 = new Bommel();
        child1.setName("Child bommel 1");
        child1.setEmoji("\uD83E\uDD7A");

        Bommel child2 = new Bommel();
        child2.setName("Child bommel 2");
        child2.setEmoji("\uD83D\uDC49");

        Bommel child3 = new Bommel();
        child3.setName("Inner child bommel 3");
        child3.setEmoji("\uD83D\uDC48");

        child1.setParent(root);
        child2.setParent(root);
        child3.setParent(child1);

        return List.of(root, child1, child2, child3);
    }

}