package com.orientechnologies.orient.core.sql;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestOrderByIndexPropDesc {

  private static final String DOCUMENT_CLASS_NAME = "MyDocument";
  private static final String PROP_INDEXED_STRING = "dateProperty";

  private ODatabaseDocument db;

  @Before
  public void init() throws Exception {
    db = new ODatabaseDocumentTx("memory:TestOrderByIndexPropDesc");
    db.create();
    OClass oclass = db.getMetadata().getSchema().createClass(DOCUMENT_CLASS_NAME);
    oclass.createProperty(PROP_INDEXED_STRING, OType.INTEGER);
    oclass.createIndex("index", INDEX_TYPE.NOTUNIQUE, PROP_INDEXED_STRING);
  }

  @After
  public void drop() {
    if (db != null) {
      db.drop();
    }
  }

  @Test
  public void worksFor1000() {
    test(1000);
  }

  @Test
  public void worksFor10000() {
    test(50000);
  }

  private void test(int count) {
    ODocument doc = db.newInstance();
    for (int i = 0; i < count; i++) {
      doc.reset();
      doc.setClassName(DOCUMENT_CLASS_NAME);
      doc.field(PROP_INDEXED_STRING, i);
      db.save(doc);
    }

    try (OResultSet result =
        db.query(
            "select from " + DOCUMENT_CLASS_NAME + " order by " + PROP_INDEXED_STRING + " desc")) {
      Assert.assertEquals(count, result.stream().count());
    }
  }
}
