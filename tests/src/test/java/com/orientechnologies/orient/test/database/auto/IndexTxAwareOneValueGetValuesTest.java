package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.common.util.ORawPair;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.*;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class IndexTxAwareOneValueGetValuesTest extends DocumentDBBaseTest {
  private static final String CLASS_NAME = "IndexTxAwareOneValueGetValuesTest";
  private static final String FIELD_NAME = "value";
  private static final String INDEX_NAME = "IndexTxAwareOneValueGetValuesTest";

  @Parameters(value = "url")
  public IndexTxAwareOneValueGetValuesTest(@Optional String url) {
    super(url);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final OSchema schema = database.getMetadata().getSchema();
    final OClass cls = schema.createClass(CLASS_NAME);
    cls.createProperty(FIELD_NAME, OType.INTEGER);
    cls.createIndex(INDEX_NAME, OClass.INDEX_TYPE.UNIQUE, FIELD_NAME);
  }

  @BeforeMethod
  @Override
  public void beforeMethod() throws Exception {
    super.beforeMethod();

    final OClass cls = database.getMetadata().getSchema().getClass(CLASS_NAME);
    cls.truncate();
  }

  @Test
  public void testPut() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();
    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();
    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges(INDEX_NAME));

    Set<OIdentifiable> resultOne = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 2);

    database.begin();

    new ODocument(CLASS_NAME).field(FIELD_NAME, 3).save();

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));

    Set<OIdentifiable> resultTwo = entrySet(index.getInternal(), Arrays.asList(1, 2, 3));
    Assert.assertEquals(resultTwo.size(), 3);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultThree = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 2);
  }

  @Test
  public void testRemove() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();
    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultOne = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 2);

    database.begin();

    try (Stream<ORID> rids = index.getInternal().getRids(1)) {
      rids.map(ORID::getRecord).forEach(record -> ((ODocument) record).delete());
    }

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultTwo = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 1);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultThree = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 2);
  }

  @Test
  public void testRemoveAndPut() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();
    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultOne = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 2);

    database.begin();

    try (Stream<ORID> ridStream = index.getInternal().getRids(1)) {
      ridStream.map(ORID::getRecord).forEach(record -> ((ODocument) record).delete());
    }
    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> resultTwo = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 2);

    database.rollback();
  }

  @Test
  public void testMultiPut() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();

    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    final ODocument document = new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    document.field(FIELD_NAME, 0);
    document.field(FIELD_NAME, 1);
    document.save();

    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> result = entrySet(index.getInternal(), Arrays.asList(1, 2));

    Assert.assertEquals(result.size(), 2);

    database.commit();

    result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 2);
  }

  @Test
  public void testPutAfterTransaction() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();

    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));

    Set<OIdentifiable> result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 2);
    database.commit();

    new ODocument(CLASS_NAME).field(FIELD_NAME, 3).save();

    result = entrySet(index.getInternal(), Arrays.asList(1, 2, 3));

    Assert.assertEquals(result.size(), 3);
  }

  @Test
  public void testRemoveOneWithinTransaction() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();

    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    try (Stream<ORID> ridStream = index.getInternal().getRids(1)) {
      ridStream.map(ORID::getRecord).forEach(record -> ((ODocument) record).delete());
    }

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 1);

    database.commit();

    result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 1);
  }

  @Test
  public void testRemoveAllWithinTransaction() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();

    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    try (Stream<ORID> ridStream = index.getInternal().getRids(1)) {
      ridStream.map(ORID::getRecord).forEach(record -> ((ODocument) record).delete());
    }

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 1);

    database.commit();

    result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 1);
  }

  @Test
  public void testPutAfterRemove() {
    if (database.getStorage().isRemote()) {
      throw new SkipException("Test is enabled only for embedded database");
    }

    database.begin();

    final OIndex index =
        database.getMetadata().getIndexManagerInternal().getIndex(database, INDEX_NAME);
    Assert.assertTrue(index instanceof OIndexTxAwareOneValueOriginalKey);

    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();
    new ODocument(CLASS_NAME).field(FIELD_NAME, 2).save();

    try (Stream<ORID> ridStream = index.getInternal().getRids(1)) {
      ridStream.map(ORID::getRecord).forEach(record -> ((ODocument) record).delete());
    }
    new ODocument(CLASS_NAME).field(FIELD_NAME, 1).save();

    Assert.assertNotNull(database.getTransaction().getIndexChanges(INDEX_NAME));
    Set<OIdentifiable> result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 2);

    database.commit();

    result = entrySet(index.getInternal(), Arrays.asList(1, 2));
    Assert.assertEquals(result.size(), 2);
  }

  private static Set<OIdentifiable> entrySet(
      final IndexInternal indexInternal, final Collection<Integer> keys) {
    if (indexInternal instanceof IndexInternalOriginalKey) {
      final IndexInternalOriginalKey indexInternalOriginalKey =
          (IndexInternalOriginalKey) indexInternal;
      try (final Stream<ORawPair<Object, ORID>> stream =
          indexInternalOriginalKey.streamEntries(keys, true)) {
        return stream.map(pair -> pair.second).collect(Collectors.toSet());
      }
    } else {
      final IndexInternalBinaryKey indexInternalBinaryKey = (IndexInternalBinaryKey) indexInternal;
      try (final Stream<ORawPair<byte[], ORID>> stream =
          indexInternalBinaryKey.streamEntries(keys, true)) {
        return stream.map(pair -> pair.second).collect(Collectors.toSet());
      }
    }
  }
}
