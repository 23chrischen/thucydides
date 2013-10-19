package net.thucydides.core.requirements.reports;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.issues.IssueTracking;
import net.thucydides.core.model.OutcomeCounter;
import net.thucydides.core.model.Release;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestTag;
import net.thucydides.core.model.TestType;
import net.thucydides.core.releases.ReleaseManager;
import net.thucydides.core.reports.TestOutcomes;
import net.thucydides.core.reports.html.ReportNameProvider;
import net.thucydides.core.requirements.RequirementsTagProvider;
import net.thucydides.core.requirements.model.Requirement;
import net.thucydides.core.util.EnvironmentVariables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sum;
import static org.hamcrest.Matchers.hasItem;

/**
 * A set of test results for a list of high-level requirements.
 */
public class RequirementsOutcomes {
    private final List<RequirementOutcome> requirementOutcomes;
    private final TestOutcomes testOutcomes;
    private final Optional<Requirement> parentRequirement;
    private final EnvironmentVariables environmentVariables;
    private final IssueTracking issueTracking;
    private final List<RequirementsTagProvider> requirementsTagProviders;
    private final ReleaseManager releaseManager;

    public final static Integer DEFAULT_TESTS_PER_REQUIREMENT = 4;

    public RequirementsOutcomes(List<Requirement> requirements,
                                TestOutcomes testOutcomes,
                                IssueTracking issueTracking,
                                EnvironmentVariables environmentVariables,
                                List<RequirementsTagProvider> requirementsTagProviders) {
        this(null, requirements, testOutcomes, issueTracking, environmentVariables, requirementsTagProviders);
    }

    public RequirementsOutcomes(Requirement parentRequirement, List<Requirement> requirements, TestOutcomes testOutcomes,
                                IssueTracking issueTracking, EnvironmentVariables environmentVariables,
                                List<RequirementsTagProvider> requirementsTagProviders) {
        this.testOutcomes = testOutcomes;
        this.parentRequirement = Optional.fromNullable(parentRequirement);
        this.environmentVariables = environmentVariables;
        this.issueTracking = issueTracking;
        this.requirementsTagProviders = requirementsTagProviders;
        this.requirementOutcomes = buildRequirementOutcomes(requirements, requirementsTagProviders);
        this.releaseManager = new ReleaseManager(environmentVariables, new ReportNameProvider());
    }

    private List<RequirementOutcome> buildRequirementOutcomes(List<Requirement> requirements,
                                                              List<RequirementsTagProvider> requirementsTagProviders) {
        List<RequirementOutcome> outcomes = Lists.newArrayList();
        for (Requirement requirement : requirements) {
            buildRequirements(outcomes, requirementsTagProviders, requirement);
        }
        return outcomes;
    }

    private void buildRequirements(List<RequirementOutcome> outcomes, List<RequirementsTagProvider> requirementsTagProviders, Requirement requirement) {
        TestOutcomes outcomesForRequirement = testOutcomes.forRequirement(requirement);

        int requirementsWithoutTests = countRequirementsWithoutTestsIn(requirement);
        int estimatedUnimplementedTests = requirementsWithoutTests * estimatedTestsPerRequirement();
        outcomes.add(new RequirementOutcome(requirement, outcomesForRequirement, requirementsWithoutTests,
                                            estimatedUnimplementedTests, issueTracking));
    }

    private int countRequirementsWithoutTestsIn(Requirement rootRequirement) {
        List<Requirement> flattenedRequirements = getFlattenedRequirements(rootRequirement);

        int requirementsWithoutTests = 0;
        for(Requirement requirement : flattenedRequirements) {
            TestOutcomes matchingOutcomes = testOutcomes.withTag(requirement.asTag());
            if (matchingOutcomes.getTotal() == 0) {
                requirementsWithoutTests++;
            }
        }
        return requirementsWithoutTests;
    }

    public int getFlattenedRequirementCount() {
        int requirementCount = 0;
        for (RequirementOutcome requirement : requirementOutcomes) {
            requirementCount += requirement.getFlattenedRequirementCount();

        }
        return requirementCount;
    }

    private List<Requirement> getFlattenedRequirements(Requirement rootRequirement) {
        List<Requirement> flattenedRequirements = Lists.newArrayList();
        flattenedRequirements.add(rootRequirement);
        flattenedRequirements.addAll(rootRequirement.getNestedChildren());
        return flattenedRequirements;
    }

    public Optional<Requirement> getParentRequirement() {
        return parentRequirement;
    }

    public int getRequirementCount() {
        return requirementOutcomes.size();
    }

    public List<RequirementOutcome> getRequirementOutcomes() {
        return ImmutableList.copyOf(requirementOutcomes);
    }

    public String getType() {
        if (requirementOutcomes.isEmpty()) {
            return "requirement";
        } else {
            return requirementOutcomes.get(0).getRequirement().getType();
        }
    }

    public String getChildrenType() {
        return typeOfFirstChildPresent();
    }

    private String typeOfFirstChildPresent() {
        for (RequirementOutcome outcome : requirementOutcomes) {
            if (!outcome.getRequirement().getChildren().isEmpty()) {
                Requirement firstChildRequirement = outcome.getRequirement().getChildren().get(0);
                return firstChildRequirement.getType();
            }
        }
        return null;
    }

    public TestOutcomes getTestOutcomes() {
        return testOutcomes;
    }

    @Override
    public String toString() {
        return "RequirementsOutcomes{" +
                "requirementOutcomes=" + requirementOutcomes +
                ", parentRequirement=" + parentRequirement +
                '}';
    }

    public int getCompletedRequirementsCount() {
        int completedRequirements = 0;
        for (RequirementOutcome requirementOutcome : requirementOutcomes) {
            if (requirementOutcome.isComplete()) {
                completedRequirements++;
            }
        }
        return completedRequirements;
    }

    public int getFailingRequirementsCount() {
        int failingRequirements = 0;
        for (RequirementOutcome requirementOutcome : requirementOutcomes) {
            if (requirementOutcome.isFailure()) {
                failingRequirements++;
            }
        }
        return failingRequirements;
    }

    public int getPendingRequirementsCount() {
        int total = 0;
        for (RequirementOutcome requirementOutcome : requirementOutcomes) {
            if (requirementOutcome.isPending()) {
                total++;
            }
        }
        return total;
    }

    public int getRequirementsWithoutTestsCount() {
        int requirementsWithNoTests = 0;
        List<RequirementOutcome> flattenedRequirementOutcomes = getFlattenedRequirementOutcomes();

        for (Requirement requirement : getAllRequirements()) {
            if (!testsRecordedFor(flattenedRequirementOutcomes, requirement)) {
                requirementsWithNoTests++;
            }
        }
        return requirementsWithNoTests;
    }

    private boolean testsRecordedFor(List<RequirementOutcome> outcomes, Requirement requirement) {
        for (RequirementOutcome outcome : outcomes) {

            if (outcome.testsRequirement(requirement) && outcome.getTestCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private List<Requirement> getAllRequirements() {
        List<Requirement> allRequirements = Lists.newArrayList();
        for (RequirementOutcome outcome : requirementOutcomes) {
            addFlattenedRequirements(outcome.getRequirement(), allRequirements);
        }
        return ImmutableList.copyOf(allRequirements);
    }

    private void addFlattenedRequirements(Requirement requirement, List<Requirement> allRequirements) {
        allRequirements.add(requirement);
        for (Requirement child : requirement.getChildren()) {
            addFlattenedRequirements(child, allRequirements);
        }
    }

    List<RequirementOutcome> flattenedRequirementOutcomes = null;

    public List<RequirementOutcome> getFlattenedRequirementOutcomes() {
        if (flattenedRequirementOutcomes == null) {
            flattenedRequirementOutcomes = getFlattenedRequirementOutcomes(requirementOutcomes);
        }
        return flattenedRequirementOutcomes;
    }

    public List<RequirementOutcome> getFlattenedRequirementOutcomes(List<RequirementOutcome> outcomes) {
        Set<RequirementOutcome> flattenedOutcomes = Sets.newHashSet();


        for (RequirementOutcome requirementOutcome : outcomes) {
            flattenedOutcomes.add(requirementOutcome);
            List<Requirement> nestedRequirements = requirementOutcome.getFlattenedRequirements();
            for(Requirement requirement : nestedRequirements) {
                TestTag requirementTag = TestTag.withName(requirement.getName()).andType(requirement.getType());
                TestOutcomes testOutcomesForRequirement = requirementOutcome.getTestOutcomes().withTag(requirementTag);
                List<Requirement> childRequirements = requirement.getChildren();

                RequirementsOutcomes childOutcomes =
                        new RequirementsOutcomes(childRequirements, testOutcomesForRequirement, issueTracking,
                                                 environmentVariables, requirementsTagProviders);
                flattenedOutcomes.addAll(getFlattenedRequirementOutcomes(childOutcomes.getRequirementOutcomes()));
            }
//
//            Requirement requirement = requirementOutcome.getRequirement();
//            if (requirement.hasChildren()) {
//                for (Requirement childRequirement : requirement.getChildren()) {
//                    TestOutcomes testOutcomesForChildRequirement = requirementOutcome.getTestOutcomes().withTag(childRequirement.getName());
//                    List<Requirement> childRequirements = childRequirement.getChildren();
//                    RequirementsOutcomes childOutcomes =
//                            new RequirementsOutcomes(childRequirement, childRequirements,
//                                                     testOutcomesForChildRequirement, issueTracking,
//                                                     environmentVariables, requirementsTagProviders);
//                    flattenedOutcomes.addAll(getFlattenedRequirementOutcomes(childOutcomes.getRequirementOutcomes()));
//                }
//            }
        }

        return ImmutableList.copyOf(flattenedOutcomes);
    }

    public OutcomeCounter getTotal() {
        return count(TestType.ANY);
    }

    public OutcomeCounter count(TestType testType) {
        return new OutcomeCounter(testType, getTestOutcomes());
    }

    public OutcomeCounter count(String testType) {
        return count(TestType.valueOf(testType.toUpperCase()));
    }

    public int getTotalTestCount() {
        return testOutcomes.getTotal();
    }

    /**
     * @return Formatted version of the test coverage metrics
     */
    public RequirementsPercentageFormatter getFormattedPercentage() {
        return new RequirementsPercentageFormatter(getProportion());
    }

    public RequirementsPercentageFormatter getFormattedPercentage(String testType) {
        return new RequirementsPercentageFormatter(proportionOf(testType));
    }

    public RequirementsPercentageFormatter getFormattedPercentage(TestType testType) {
        return new RequirementsPercentageFormatter(proportionOf(testType));
    }

    private int totalEstimatedAndImplementedTests() {
        int totalImplementedTests = getTotalTestCount();
        return totalImplementedTests + getEstimatedUnimplementedTests();
    }

    public int getEstimatedUnimplementedTests() {
        return getRequirementsWithoutTestsCount() * estimatedTestsPerRequirement();
    }

    private int estimatedTestsPerRequirement() {
        return environmentVariables.getPropertyAsInteger(ThucydidesSystemProperty.ESTIMATED_TESTS_PER_REQUIREMENT.toString(),
                DEFAULT_TESTS_PER_REQUIREMENT);
    }

    public RequirementsProportionCounter getProportion() {
        return proportionOf(TestType.ANY);
    }

    public RequirementsProportionCounter proportionOf(String testType) {
        return proportionOf(TestType.valueOf(testType.toUpperCase()));
    }

    public RequirementsProportionCounter proportionOf(TestType testType) {
        return new RequirementsProportionCounter(testType, testOutcomes, totalEstimatedAndImplementedTests());
    }

    public RequirementsOutcomes getReleasedRequirementsFor(Release release) {
        List<Requirement> matchingRequirements = Lists.newArrayList();
        List<TestOutcome> matchingTestOutcomes = Lists.newArrayList();
        List<List<Requirement>> matchingRequirementPaths = Lists.newArrayList();

        // Add all test outcomes with a matching release

        List<RequirementOutcome> requirementOutcomes
                = releaseManager.enrichRequirementsOutcomesWithReleaseTags(getRequirementOutcomes());

        for(RequirementOutcome outcome : requirementOutcomes) {//  getFlattenedRequirementOutcomes()) {
            Collection<String> releaseVersions = outcome.getReleaseVersions();
            if (releaseVersions.contains(release.getName())) {
                List<TestOutcome> outcomesForRelease = outcomesForRelease(outcome.getTestOutcomes().getOutcomes(), release.getName());
                matchingTestOutcomes.addAll(outcomesForRelease);
                matchingRequirements.add(outcome.getRequirement());
            }
        }
        matchingRequirements = removeRequirementsWithoutTestsFrom(matchingRequirements);
        return new RequirementsOutcomes(Lists.newArrayList(matchingRequirements),
                                        TestOutcomes.of(matchingTestOutcomes),
                                        issueTracking,
                                        environmentVariables,
                                        requirementsTagProviders);
    }

    private List<Requirement> removeRequirementsWithoutTestsFrom(List<Requirement> requirements) {
        List<Requirement> prunedRequirements = Lists.newArrayList();
        for(Requirement requirement : requirements) {
            if (testsExistFor(requirement)) {
                List<Requirement> prunedChildren = removeRequirementsWithoutTestsFrom(requirement.getChildren());
                prunedRequirements.add(requirement.withChildren(prunedChildren));
            }
        }
        return ImmutableList.copyOf(prunedRequirements);
    }

    private boolean testsExistFor(Requirement requirement) {
        TestTag requirementTag = TestTag.withName(requirement.getName()).andType(requirement.getType());
        return !getTestOutcomes().withTag(requirementTag).getOutcomes().isEmpty();
    }

    private List<TestOutcome> outcomesForRelease(List<? extends TestOutcome> outcomes,
                                                 String releaseName) {
        releaseManager.enrichOutcomesWithReleaseTags(outcomes);
        return (List<TestOutcome>) filter(having(on(TestOutcome.class).getVersions(), hasItem(releaseName)), outcomes);
    }
}
