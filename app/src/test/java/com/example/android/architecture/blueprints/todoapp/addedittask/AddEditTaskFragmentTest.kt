/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.addedittask

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.ServiceLocator
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.ADD_EDIT_RESULT_OK
import com.example.android.architecture.blueprints.todoapp.util.getTasksBlocking
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.TextLayoutMode

/**
 * Integration test for the Add Task screen.
 */
@ObsoleteCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
@LooperMode(LooperMode.Mode.PAUSED)
@TextLayoutMode(TextLayoutMode.Mode.REALISTIC)
class AddEditTaskFragmentTest {
    private lateinit var repository: TasksRepository

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val taskTitle = "title"
    private val taskDescription = "description"

    @Before
    fun initRepository() {
        repository = ServiceLocator.provideTasksRepository(getApplicationContext())
    }

    @After
    fun cleanupDb() = runBlocking {
        ServiceLocator.resetForTests()
    }

    @Test
    fun emptyTask_isNotSaved() {
        // GIVEN - On the "Add Task" screen.
        val bundle = AddEditTaskFragmentArgs(null,
            getApplicationContext<Context>().getString(R.string.add_task)).toBundle()
        launchFragmentInContainer<AddEditTaskFragment>(bundle, R.style.AppTheme)

        // WHEN - Enter invalid title and description combination and click save
        onView(withId(R.id.add_task_title)).perform(clearText())
        onView(withId(R.id.add_task_description)).perform(clearText())
        onView(withId(R.id.fab_save_task)).perform(click())

        // THEN - Entered Task is still displayed (a correct task would close it).
        onView(withId(R.id.add_task_title)).check(matches(isDisplayed()))
    }

    @Test
    fun validTask_isSaved() {
        // GIVEN - On the "Add Task" screen.
        val navController = mock(NavController::class.java)
        launchFragment(navController)

        // WHEN - Valid title and description combination and click save
        saveNewTask()

        // THEN - Verify that the repository saved the task
        val tasks = (repository.getTasksBlocking(true) as Result.Success).data
        assertThat(tasks).hasSize(1)
        assertThat(tasks[0].title).isEqualTo(taskTitle)
        assertThat(tasks[0].description).isEqualTo(taskDescription)
    }

    @Test
    fun validTask_navigatesBack() {
        // GIVEN - On the "Add Task" screen.
        val navController = mock(NavController::class.java)
        launchFragment(navController)

        // WHEN - Valid title and description combination and click save
        saveNewTask()

        repository.getTasksBlocking(true)

        // THEN - Verify that we navigated back to the tasks screen.
        verify(navController).navigate(
            AddEditTaskFragmentDirections
                .actionAddEditTaskFragmentToTasksFragment(ADD_EDIT_RESULT_OK))
    }

    private fun saveNewTask() {
        onView(withId(R.id.add_task_title))
            .perform(replaceText(taskTitle)) // Type new task title
        onView(withId(R.id.add_task_description)).perform(
            replaceText(taskDescription)) // Type new task description and close the keyboard
        onView(withId(R.id.fab_save_task)).perform(click())
    }

    private fun launchFragment(navController: NavController?) {
        val bundle = AddEditTaskFragmentArgs(null,
            getApplicationContext<Context>().getString(R.string.add_task)).toBundle()
        val scenario = launchFragmentInContainer<AddEditTaskFragment>(bundle, R.style.AppTheme)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
    }

}
