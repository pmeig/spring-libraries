package pmeig.spring.libraries.jpa.data.bigquery

import com.google.api.gax.paging.Page
import com.google.api.gax.paging.Pages
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList

private val emptyPage = Pages.empty<FieldValueList>()

class BigQueryPage(
  private val fields: List<FieldValue>
): Page<FieldValueList> {
  override fun hasNextPage(): Boolean {
    return emptyPage.hasNextPage()
  }

  override fun getNextPageToken(): String? {
    return emptyPage.nextPageToken
  }

  override fun getNextPage(): Page<FieldValueList?>? {
    return emptyPage.nextPage
  }

  override fun iterateAll(): Iterable<FieldValueList> {
    return values
  }

  override fun getValues(): Iterable<FieldValueList> {
    return listOf(FieldValueList.of(fields))
  }
}