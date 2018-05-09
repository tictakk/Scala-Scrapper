import java.lang.Exception
import java.sql
import java.sql.SQLException

import javax.xml.transform.Source
import com.sun.org.apache.xalan.internal.xsltc.dom.MatchingIterator

/**
  * Created by takk on 12/13/16.
  */
object GatherTeamData {

  def main(args: Array[String]): Unit = {
    import scala.io.Source
//    val data = Source.fromURL("http://www.espn.com/college-football/scoreboard/_/group/80/year/2016/seasontype/2/week/1").mkString

    val data = Source.fromURL("http://www.espn.com/college-football/schedule/_/week/2").mkString //change week here manually
    val pat = """college-football\/game\?gameId=([0-9]+)""".r
    val college = """college-football\/game\?gameId="""
    val meta = """([^a-zA-Z]+)"""
    val lst = pat.findAllIn(data).toList

    val getIDs: List[String] = lst.flatMap(l => l.split(college)).filterNot(_.isEmpty)
    val teamSetList = getIDs.map(getTeamName(_))
    teamSetList.foreach(x => println(x.head + ":"+x.last))


  }

  case class Team(teamName: String, score: String, yards: String)

  def testGetStats(id: String): Set[String] =
  {
    val url = "http://www.espn.com/college-football/matchup?gameId="
    import scala.io.Source
    val matchupInfo = Source.fromURL(url.concat(id)).mkString
    val index1 = matchupInfo.indexOf("<tr class=\"highlight\" data-stat-attr=\"totalYards\">")
    val sub = matchupInfo.substring(index1,index1+201)
    val removeFrom = sub.indexOf("""</td>""")
    val newSub = sub.substring(removeFrom, sub.length-5)
    val scores = """([0-9]+)""".r.findAllIn(newSub)
    scores.toSet
  }

  def testGetScore(id: String): Set[String] =
  {
    //will fix this ugliness later
    val url = "http://www.espn.com/college-football/game?gameId="
    import scala.io.Source
    val matchupInfo = Source.fromURL(url.concat(id)).mkString
    val pat = """<td class="final-score">([0-9]+)</td>""".r
    val test = pat.findAllIn(matchupInfo).toList.filterNot(_.isEmpty).toList.distinct //.flatMap(t => "([0-9]+)".r.split(t))
    val test1 = test.flatMap(t => "([0-9]+)+".r.split(t))
    val one = test1(0).r
    val scores = test.flatMap(t => one.split(t)).filterNot(_.isEmpty)
    val a = scores(0).substring(0,scores(0).indexOf("</td>")).trim
    val b = scores(1).substring(0,scores(1).indexOf("</td>")).trim
    Set(a,b)
  }

  def getTeamName(id: String): Set[String] =
  {
    val url = "http://www.espn.com/college-football/game?gameId="
    import scala.io.Source
    val matchupInfo = Source.fromURL(url.concat(id)).mkString
    val pat = """<Attribute name="title">.+.</Attribute>""".r
    val test = pat.findAllIn(matchupInfo).toList
    val str: String = test(0)
    val one = str.substring(str.indexOf(">")+1,str.indexOf(" vs.")).trim
    val two = str.substring(str.indexOf("vs. ")+4, str.indexOf(" -")).trim
    Set(one, two)
  }

  def updateDB(team1: Team, team2: Team): Unit =
  {
    import java.sql.Connection
    import java.sql.DriverManager
    import java.sql.SQLException

    try {
      //not prod db
      val conn: Connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/web", "webber", "matthew")
      val query = "UPDATE MasterGamesList SET score_1=" + team1.score + ", score_2=" + team2.score + ", offense_1=" + team1.yards + ", offense_2=" + team2.yards + " WHERE team_1=(SELECT team_id FROM Teams WHERE name='" + team1.teamName + "') AND team_2=(SELECt team_id FROM Teams WHERE name='" + team2.teamName + "') OR team_1=(SELECT team_id FROM Teams WHERE name='" + team2.teamName + "') AND team_2=(SELECT team_id FROM Teams WHERE name='" + team1.teamName + "')"
      conn.createStatement().executeUpdate(query);
      print("Done")
    }catch {
      //      import scala.Exception
      case e: Exception => println("gen exception " + e)
      case sql: SQLException => println("sql exception " + sql)
      case _ => println("dunno")
    }
  }

  def addTeamsToSchedule(teamOne: String, teamTwo: String): Unit =
  {
    import java.sql.Connection
    import java.sql.DriverManager
    import java.sql.SQLException

    try{
      //not prod
      val conn: Connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/CFB","root","root")
      val ps = conn.prepareStatement("INSERT INTO MATCHUP VALUES(?, ?, ?)")
      ps.setString(1, "NULL")
      ps.setString(2, ("SELECT TEAM_ID FROM TEAMS WHERE NAME='"+teamOne+"'"))
      ps.setString(3, ("SELECT TEAM_ID FROM TEAMS WHERE NAME='"+teamTwo+"'"))

      ps.close()
      conn.close()
    }catch{
      case s: SQLException => println("sql exception: "+s)
      case _ => println("unknown error"+_)
    }
  }
}
