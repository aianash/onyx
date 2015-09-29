package onyx
package core

import scala.util.Random
import scala.reflect.ClassTag

import scalaz._, Scalaz._


/** Base trait for all random iterators
  * that is used to iterate over elements of a collection
  * randomly
  * It also defines two methods for generating random combinations
  * of elements from two collections
  */
trait RandomIterator[+T] extends Iterator[T] {
  def random: Random
  def |+|[U : ClassTag, R, CC[X] <: TraversableOnce[X]](another: CC[U])(f: (T, U) => R): RandomIterator[R]
  def <+>[U : ClassTag, CC[X] <: TraversableOnce[X]](another: CC[U]): RandomIterator[(T, U)] = (this |+| another)(_ -> _)
}


/** These iterators has 'hasNext' set to always true
  * therefore they generate infinite random elements
  */
trait InfiniteRandomIterator[T] extends RandomIterator[T] {
  final def hasNext = true
}


/** Infinite random iterator, generating nulls
  */
object NullRandomIterator extends InfiniteRandomIterator[Null] {
  val random = Random

  def |+|[U : ClassTag, R, CC[X] <: TraversableOnce[X]](another: CC[U])(f: (Null, U) => R) =
    NullRandomIterator.asInstanceOf[RandomIterator[R]]

  def next = null
}


/** This is window based random iterator.
  * This works by first generating combinations from a windowSize cached elements
  * and then refreshing it randomly once a threshold of number of combinations to
  * generate is reached.
  */
class WindowedRandomIterator[P, S : ClassTag, R, CC[X] <: TraversableOnce[X]] private (
  previous: InfiniteRandomIterator[P],
  source: CC[S],
  f: (P, S) => R,
  val random: Random,
  windowSize: Int,
  threshold: Float) extends InfiniteRandomIterator[R] {

  private val sourceSize = source.size
  private val window = Array.ofDim[S](windowSize)
  private var nxtStartIndex = 0
  private var fillSize = 0
  private var numGenerated = 0

  refreshWindow()

  def |+|[S2 : ClassTag, R2, CC2[X] <: TraversableOnce[X]](another: CC2[S2])(f: (R, S2) => R2): RandomIterator[R2] =
    new WindowedRandomIterator[R, S2, R2, CC2](this, another, f, this.random, this.windowSize, this.threshold)

  def next = {
    if(shouldRefresh()) refreshWindow()
    val p = previous.next
    val s = window(random.nextInt(fillSize))
    numGenerated += 1
    f(p, s)
  }

  // refresh elements in window
  private def refreshWindow() {
    fillSize = math.min(windowSize, sourceSize - nxtStartIndex)
    source.copyToArray(window, nxtStartIndex, fillSize)
    nxtStartIndex = (nxtStartIndex + fillSize) % sourceSize
    numGenerated = 0
  }

  // randomly refresh window if threshold is reached
  private def shouldRefresh() =
    if((numGenerated.toFloat / fillSize) > threshold) Random.nextBoolean
    else false

}


object WindowedRandomIterator {

  /** Create a window based random iterator with default configs
    *
    * @param xs - collection to iterate
    * @return RandomIterator - window based random iterator
    */
  def apply[S: ClassTag, CC[X] <: TraversableOnce[X]](xs: CC[S]): RandomIterator[S] =
    apply(xs, Random, bestWindowSize(xs.size), 0.5f)


  /** Create a window based random iterator with given config
    *
    * @param xs - collection to iterate
    * @param random - random number generator
    * @param windowSize - size of the window to be used
    * @param threshold - threshold when to shift the windows
    * @return RandomIterator - window based random iterator
    */
  def apply[S: ClassTag, CC[X] <: TraversableOnce[X]](xs: CC[S], random: Random, windowSize: Int, threshold: Float): RandomIterator[S] =
    new WindowedRandomIterator(
      previous   = NullRandomIterator,
      source     = xs,
      f          = { (_: Null, s: S) => s },
      random     = random,
      windowSize = windowSize,
      threshold  = threshold)

  private def bestWindowSize(sourceSize: Int) = {
    var wsize = sourceSize >> 4
    if(wsize < 2) wsize = sourceSize >> 2
    if(wsize < 2) wsize = sourceSize
    wsize
  }

}