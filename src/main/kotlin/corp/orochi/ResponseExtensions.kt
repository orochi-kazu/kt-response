package corp.orochi

inline fun <A, B, reified Final> combine(
  rA: Response<A>,
  rB: Response<B>,
  crossinline final: (A, B) -> Final
): Response<Final> =
  rA flatMap { a ->
    rB map { b ->
      final(a, b)
    }
  }

inline fun <A, B, C, reified Final> combine(
  rA: Response<A>,
  rB: Response<B>,
  rC: Response<C>,
  crossinline final: (A, B, C) -> Final
): Response<Final> =
  rA flatMap { a ->
    rB flatMap { b ->
      rC map { c ->
        final(a, b, c)
      }
    }
  }
