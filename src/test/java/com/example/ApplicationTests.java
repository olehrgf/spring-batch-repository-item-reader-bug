package com.example;

import java.util.Date;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.model.Element;
import com.example.model.ElementRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {BatchTestConfiguration.class, ApplicationTests.class})
@Configuration
@ActiveProfiles({"test"})
public class ApplicationTests {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job jobWithException;

    @Test
    public void contextLoads() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("timestamp", String.valueOf(new Date().getTime()))
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(jobWithException, jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getAllFailureExceptions()).hasSize(1);
        assertThat(jobExecution.getAllFailureExceptions().get(0)).isInstanceOf(Error.class);

        jobExecution = jobLauncherTestUtils.getJobLauncher().run(jobWithException, jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getAllFailureExceptions()).hasSize(1);
        assertThat(jobExecution.getAllFailureExceptions().get(0)).isInstanceOf(RuntimeException.class);
    }

    @Configuration
    public class JobConfiguration {

        @Autowired
        private JobBuilderFactory jobBuilderFactory;

        @Autowired
        private StepBuilderFactory stepBuilderFactory;

        @Autowired
        private ElementRepository elementRepository;

        @Bean
        RepositoryItemReader<Element> repositoryItemReader() {
            RepositoryItemReader<Element> repositoryItemReader = new RepositoryItemReader<>();
            repositoryItemReader.setRepository(elementRepository);
            repositoryItemReader.setMethodName("findAll");
            repositoryItemReader.setPageSize(10);
            final HashMap<String, Sort.Direction> sorts = new HashMap<>();
            sorts.put("id", Sort.Direction.ASC);
            repositoryItemReader.setSort(sorts);
            return repositoryItemReader;
        }

        @Bean
        RepositoryItemWriter<Element> repositoryItemWriter() {
            RepositoryItemWriter<Element> repositoryItemWriter = new RepositoryItemWriter<>();
            repositoryItemWriter.setRepository(elementRepository);
            repositoryItemWriter.setMethodName("save");
            return repositoryItemWriter;
        }

        @Bean
        public Job jobWithException() {
            return jobBuilderFactory.get("job-with-exception")
                    .start(stepBuilderFactory.get("db-initialization-step")
                            .tasklet((contribution, chunkContext) -> {
                                elementRepository.deleteAll();
                                for (int i = 1; i < 100; i++) {
                                    Element element = new Element();
                                    element.setValue(String.valueOf(i));
                                    elementRepository.save(element);
                                }
                                return RepeatStatus.FINISHED;
                            })
                            .build())
                    .next(
                            stepBuilderFactory.get("step-with-exception")
                                    .<Element, Element>chunk(10)
                                    .reader(this.repositoryItemReader())
                                    .processor(item -> {
                                        if ("processed".equals(item.getStatus())) {
                                            throw new RuntimeException(item.getValue() + " already processed");
                                        }
                                        if ("21".equals(item.getValue())) {
                                            throw new Error("test");
                                        }
                                        item.setStatus("processed");
                                        return item;
                                    })
                                    .writer(this.repositoryItemWriter())
                                    .allowStartIfComplete(true)
                                    .build()
                    )
                    .build();
        }
    }
}
