package microtools

import microtools.models.Problem

class ProblemException(val problem: Problem)
    extends RuntimeException(problem.message)
