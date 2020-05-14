package com.gzeinnumer.flatmapswitchmapconcatmapkt

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.schedulers.TestScheduler
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //https://www.journaldev.com/19300/rxjava-flatmap-switchmap-concatmap
        //https://medium.com/appunite-edu-collection/rxjava-flatmap-switchmap-and-concatmap-differences-examples-6d1f3ff88ee0

//        flatMap()
//        switchMap()
        concatMap()
//        flatMap_x_concatMap()
    }

    @SuppressLint("CheckResult")
    private fun flatMap() {
        /*
        --FlatMap--
        So what actually happened here is operator flatMap does not care about the order of the items.
        It creates a new observable for each item and that observable lives its own life.
        Some of them will emit faster, and others slower because we delay each of them for a random
        amount of seconds. Similar effect will happen without delaying, but it was added for the
        sake of an argument.

       *bla bla bla, intinya, masing2 item punya masing2 lives, atau lebih simple nya, multi Thread,
       siapa yang lebih cepat selesai, dia yang dikirim dulu, atau disimpan dulu
         */
        val race: List<String> = ArrayList(listOf("Alan", "Bob", "Cobb", "Dan", "Evan", "Finch"))
        Observable.fromIterable(race)
                .flatMap { s ->
                    val delay = Random().nextInt(5)
                    Observable.just(s).map { obj: String -> obj.toUpperCase() }
                            .delay(delay.toLong(), TimeUnit.SECONDS)
                }
                .subscribe { value: Any -> Log.d("flatMap", "onCreate: $value") }
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //Random
        //BOB
        //COBB
        //ALAN
        //DAN
        //EVAN
        //FINCH
    }


    @SuppressLint("CheckResult")
    private fun switchMap() {
        /*
        --SwitchMap--
        whenever a new item is emitted by the source Observable, it will unsubscribe to and stop
        mirroring the Observable that was generated from the previously-emitted item,
        and begin only mirroring the current one.

        *intinya. kalau ada task baru, abaikan yang lama, sesuaikan dengan posisi, yang pasti,
        yang dikerjakan pasti yang trakir
         */
        val race: List<String> = ArrayList(listOf("Alan", "Bob", "Cobb", "Dan", "Evan", "Finch(Last)"))
        val scheduler = TestScheduler()
        Observable.fromIterable(race)
                .switchMap(Function<String, Observable<*>> { s: String ->
                    val delay = Random().nextInt(2)
                    Observable.just(s).map { obj: String -> obj.toUpperCase() }
                            .delay(delay.toLong(), TimeUnit.SECONDS, scheduler)
                })
                .subscribe { value: Any -> Log.d("switchMap", "onCreate: $value") }
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)
        //Only last index
        //FINCH
    }

    @SuppressLint("CheckResult")
    private fun concatMap() {
        /*
        --ConcatMap--
        ConcatMap works almost the same as flatMap, but preserves the order of items.
        But concatMap has one big flaw: it waits for each observable to finish all
        the work until next one is processed.

        *sama dengan flat map, masing2 punya lives, tapi concatmap, mengurutkan sesuai index, kalau flatmap tidak,
         */
        val race: List<String> = ArrayList(listOf("Alan", "Bob", "Cobb", "Dan", "Evan", "Finch(Last)"))
        val scheduler = TestScheduler()
        Observable.fromIterable(race)
                .concatMap(Function<String, Observable<*>> { s: String ->
                    val delay = Random().nextInt(2)
                    Observable.just(s).map { obj: String -> obj.toUpperCase() }
                            .delay(delay.toLong(), TimeUnit.SECONDS, scheduler)
                })
                .subscribe { value: Any -> Log.d("concatMap", "onCreate: $value") }
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        //Sama dengan index
        //ALAN
        //BOB
        //COBB
        //DAN
        //EVAN
        //FINCH(LAST)
    }

    @SuppressLint("CheckResult")
    private fun flatMap_x_concatMap() {
        val items: List<String> = ArrayList(listOf("a", "b", "c", "d", "e", "f"))
        val scheduler1 = TestScheduler()
        val scheduler2 = TestScheduler()
        Observable.fromIterable(items)
                .flatMap { s: String ->
                    Observable.just(s + "x")
                            .delay(5, TimeUnit.SECONDS, scheduler1)
                            .doOnNext { Log.d("flatMap_x_concatMap1", "fc_onCreate1 : " + scheduler1.now(TimeUnit.MILLISECONDS) + " ") }
                }
                .toList()
                .subscribe()
        scheduler1.advanceTimeBy(1, TimeUnit.MINUTES)
        Observable.fromIterable(items)
                .concatMap { s: String ->
                    Observable.just(s + "x")
                            .delay(5, TimeUnit.SECONDS, scheduler2)
                            .doOnNext { Log.d("flatMap_x_concatMap2", "fc_onCreate2 : " + scheduler2.now(TimeUnit.MILLISECONDS) + " ") }
                }
                .toList()
                .subscribe()
        scheduler2.advanceTimeBy(1, TimeUnit.MINUTES)

        //never save state
        //fc_onCreate1 : 5000
        //fc_onCreate1 : 5000
        //fc_onCreate1 : 5000
        //fc_onCreate1 : 5000
        //fc_onCreate1 : 5000
        //fc_onCreate1 : 5000

        //add last state to new state
        //fc_onCreate2 : 5000
        //fc_onCreate2 : 10000
        //fc_onCreate2 : 15000
        //fc_onCreate2 : 20000
        //fc_onCreate2 : 25000
        //fc_onCreate2 : 30000
    }

}
