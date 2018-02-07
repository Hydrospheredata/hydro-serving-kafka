package io.hydrosphere.serving.kafka.utils

import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}

trait StoreWithLock[K,V]{

  private[StoreWithLock] var store: Map[K, V] = Map()
  private val lock: ReadWriteLock = new ReentrantReadWriteLock()

  def withWriteLock(func: Map[K, V] => Map[K,V]): Map[K,V] = {
    lock.writeLock().lock()
    try {
      store = func(store)
      store
    } catch {
      case e: Exception => {
        store
      }
    } finally {
      lock.writeLock().unlock()
    }
  }

  def withReadLock[T](func:Map[K,V] => T):T = {
    lock.readLock().lock()
    try{
      func(store)
    } finally {
      lock.readLock().unlock()
    }
  }

}
